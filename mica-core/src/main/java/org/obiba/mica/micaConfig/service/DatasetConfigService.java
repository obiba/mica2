/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service;

import java.io.IOException;
import java.util.Scanner;

import javax.inject.Inject;

import org.obiba.mica.micaConfig.domain.DatasetConfig;
import org.obiba.mica.micaConfig.repository.DatasetConfigRepository;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;

@Component
public class DatasetConfigService extends EntityConfigService<DatasetConfig> {

  @Inject
  DatasetConfigRepository datasetConfigRepository;

  @Override
  protected MongoRepository<DatasetConfig, String> getRepository() {
    return datasetConfigRepository;
  }

  @Override
  protected String getDefaultId() {
    return "default";
  }

  @Override
  protected DatasetConfig createDefaultForm() {
    return createDefaultDatasetForm();
  }

  private DatasetConfig createDefaultDatasetForm() {
    DatasetConfig form = new DatasetConfig();
    form.setDefinition(getDefaultDatasetFormResourceAsString("definition.json"));
    form.setSchema(getDefaultDatasetFormResourceAsString("schema.json"));
    return form;
  }

  private Resource getDefaultDatasetFormResource(String name) {
    return new DefaultResourceLoader().getResource("classpath:config/dataset-form/" + name);
  }

  private String getDefaultDatasetFormResourceAsString(String name) {
    try(Scanner s = new Scanner(getDefaultDatasetFormResource(name).getInputStream())) {
      return s.useDelimiter("\\A").hasNext() ? s.next() : "";
    } catch(IOException e) {
      return "";
    }
  }

}
