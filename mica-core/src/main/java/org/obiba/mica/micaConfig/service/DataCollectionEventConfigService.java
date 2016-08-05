package org.obiba.mica.micaConfig.service;

import java.io.IOException;
import java.util.Scanner;

import javax.inject.Inject;

import org.obiba.mica.micaConfig.domain.DataCollectionEventConfig;
import org.obiba.mica.micaConfig.repository.DataCollectionEventConfigRepository;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class DataCollectionEventConfigService extends EntityConfigService<DataCollectionEventConfig> {

  @Inject
  DataCollectionEventConfigRepository dataCollectionEventConfigRepository;

  @Override
  protected DataCollectionEventConfigRepository getRepository() {
    return dataCollectionEventConfigRepository;
  }

  @Override
  protected String getDefaultId() {
    return "default";
  }

  @Override
  protected DataCollectionEventConfig createDefaultForm() {
    return createDefaultDataCollectionEventForm();
  }

  private DataCollectionEventConfig createDefaultDataCollectionEventForm() {
    DataCollectionEventConfig form = new DataCollectionEventConfig();
    form.setDefinition(getDefaultDataCollectionEventFormResourceAsString("definition.json"));
    form.setSchema(getDefaultDataCollectionEventFormResourceAsString("schema.json"));
    return form;
  }

  private Resource getDefaultDataCollectionEventFormResource(String name) {
    return new DefaultResourceLoader().getResource("classpath:config/data-collection-event-form/" + name);
  }

  private String getDefaultDataCollectionEventFormResourceAsString(String name) {
    try(Scanner s = new Scanner(getDefaultDataCollectionEventFormResource(name).getInputStream())) {
      return s.useDelimiter("\\A").hasNext() ? s.next() : "";
    } catch(IOException e) {
      return "";
    }
  }
}
