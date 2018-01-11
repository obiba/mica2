/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.repository.DataAccessFormRepository;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Optional;
import java.util.Scanner;

import static org.springframework.util.StringUtils.isEmpty;

@Component
public class DataAccessFormService {

  @Inject
  FileStoreService fileStoreService;

  @Inject
  DataAccessFormRepository dataAccessFormRepository;

  public DataAccessForm createOrUpdate(DataAccessForm dataAccessForm) {
    validateForm(dataAccessForm);

    dataAccessForm.getPdfTemplates().forEach((k,v)-> {
      if(v.isJustUploaded()) {
        fileStoreService.save(v.getId());
        v.setJustUploaded(false);
      }
    });

    return dataAccessFormRepository.save(dataAccessForm);
  }

  public Optional<DataAccessForm> find() {
    DataAccessForm form = dataAccessFormRepository.findOne(DataAccessForm.DEFAULT_ID);
    if (form == null) {
      createOrUpdate(createDefaultDataAccessForm());
      form = dataAccessFormRepository.findOne(DataAccessForm.DEFAULT_ID);
    }
    if (StringUtils.isEmpty(form.getCsvExportFormat())) {
      form.setCsvExportFormat(getDefaultDataAccessFormResourceAsString("export-csv-schema.json"));
      form = createOrUpdate(form);
    }

    return Optional.ofNullable(form);
  }

  private void validateForm(DataAccessForm dataAccessForm) {
    validateJsonObject(dataAccessForm.getSchema());
    validateJsonArray(dataAccessForm.getDefinition());
    if (!isEmpty(dataAccessForm.getCsvExportFormat())) {
      validateJsonObject(dataAccessForm.getCsvExportFormat());
    }
  }

  private void validateJsonObject(String json) {
    try {
      new JSONObject(json);
    } catch(JSONException e) {
      throw new InvalidFormSchemaException(e);
    }
  }
  private void validateJsonArray(String json) {
    try {
      new JSONArray(json);
    } catch(JSONException e) {
      throw new InvalidFormSchemaException(e);
    }
  }

  private DataAccessForm createDefaultDataAccessForm() {
    DataAccessForm form = new DataAccessForm();
    form.setDefinition(getDefaultDataAccessFormResourceAsString("definition.json"));
    form.setSchema(getDefaultDataAccessFormResourceAsString("schema.json"));
    form.setCsvExportFormat(getDefaultDataAccessFormResourceAsString("export-csv-schema.json"));
    form.setTitleFieldPath("projectTitle");
    form.setSummaryFieldPath("summary");
    return form;
  }

  private Resource getDefaultDataAccessFormResource(String name) {
    return new DefaultResourceLoader().getResource("classpath:config/data-access-form/" + name);
  }

  private String getDefaultDataAccessFormResourceAsString(String name) {
    try(Scanner s = new Scanner(getDefaultDataAccessFormResource(name).getInputStream())) {
      return s.useDelimiter("\\A").hasNext() ? s.next() : "";
    } catch(IOException e) {
      return "";
    }
  }
}
