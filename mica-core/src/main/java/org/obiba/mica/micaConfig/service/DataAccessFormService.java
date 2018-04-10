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

import java.util.Optional;

import javax.inject.Inject;

import org.obiba.mica.file.FileStoreService;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.repository.DataAccessFormRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DataAccessFormService extends AbstractDataAccessEntityFormService<DataAccessForm> {

  private FileStoreService fileStoreService;

  private DataAccessFormRepository dataAccessFormRepository;

  @Inject
  public DataAccessFormService(FileStoreService fileStoreService, DataAccessFormRepository dataAccessFormRepository) {
    this.fileStoreService = fileStoreService;
    this.dataAccessFormRepository = dataAccessFormRepository;
  }

  @Override
  String getDataAccessEntityFormResourceLocation() {
    return "classpath:config/data-access-form/";
  }

  @Override
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

  @Override
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

  private DataAccessForm createDefaultDataAccessForm() {
    DataAccessForm form = new DataAccessForm();
    form.setDefinition(getDefaultDataAccessFormResourceAsString("definition.json"));
    form.setSchema(getDefaultDataAccessFormResourceAsString("schema.json"));
    form.setCsvExportFormat(getDefaultDataAccessFormResourceAsString("export-csv-schema.json"));
    form.setTitleFieldPath("projectTitle");
    form.setSummaryFieldPath("summary");
    return form;
  }
}
