package org.obiba.mica.micaConfig.service;

import javax.inject.Inject;

import org.obiba.mica.micaConfig.domain.HarmonizationPopulationConfig;
import org.obiba.mica.micaConfig.repository.HarmonizationPopulationConfigRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;

@Component
public class HarmonizationPopulationConfigService extends EntityConfigService<HarmonizationPopulationConfig> {

  @Inject
  HarmonizationPopulationConfigRepository harmonizationPopulationConfigRepository;

  @Override
  protected MongoRepository<HarmonizationPopulationConfig, String> getRepository() {
    return harmonizationPopulationConfigRepository;
  }

  @Override
  protected String getDefaultId() {
    return "default";
  }

  @Override
  protected HarmonizationPopulationConfig createEmptyForm() {
    return new HarmonizationPopulationConfig();
  }

  @Override
  protected String getDefaultSchemaResourcePath() {
    return "classpath:config/population-form/harmonization-schema.json";
  }

  @Override
  protected String getMandatorySchemaResourcePath() {
    return "classpath:config/population-form/schema-mandatory.json";
  }

  @Override
  protected String getDefaultDefinitionResourcePath() {
    return "classpath:config/population-form/harmonization-definition.json";
  }

  @Override
  protected String getMandatoryDefinitionResourcePath() {
    return "classpath:config/population-form/definition-mandatory.json";
  }
}
