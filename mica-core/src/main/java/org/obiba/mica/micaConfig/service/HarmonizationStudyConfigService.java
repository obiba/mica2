package org.obiba.mica.micaConfig.service;

import javax.inject.Inject;

import org.obiba.mica.micaConfig.HarmonizationStudyConfig;
import org.obiba.mica.micaConfig.repository.HarmonizationConfigRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;

@Component
public class HarmonizationStudyConfigService extends EntityConfigService<HarmonizationStudyConfig> {

  @Inject
  HarmonizationConfigRepository harmonizationConfigRepository;

  @Override
  protected MongoRepository<HarmonizationStudyConfig, String> getRepository() {
    return harmonizationConfigRepository;
  }

  @Override
  protected String getDefaultId() {
    return "default";
  }

  @Override
  protected HarmonizationStudyConfig createEmptyForm() {
    return new HarmonizationStudyConfig();
  }

  @Override
  protected String getDefaultSchemaResourcePath() {
    return "classpath:config/study-form/harmonization-schema.json";
  }

  @Override
  protected String getMandatorySchemaResourcePath() {
    return "classpath:config/study-form/schema-mandatory.json";
  }

  @Override
  protected String getDefaultDefinitionResourcePath() {
    return "classpath:config/study-form/harmonization-definition.json";
  }

  @Override
  protected String getMandatoryDefinitionResourcePath() {
    return "classpath:config/study-form/definition-mandatory.json";
  }
}
