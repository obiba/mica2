package org.obiba.mica.micaConfig.service;

import java.io.IOException;
import java.util.Scanner;

import javax.inject.Inject;

import org.obiba.mica.micaConfig.domain.StudyConfig;
import org.obiba.mica.micaConfig.repository.StudyConfigRepository;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class StudyConfigService extends EntityConfigService<StudyConfig> {

  @Inject
  StudyConfigRepository studyConfigRepository;

  @Override
  protected StudyConfigRepository getRepository() {
    return studyConfigRepository;
  }

  @Override
  protected String getDefaultId() {
    return "default";
  }

  @Override
  protected StudyConfig createDefaultForm() {
    return createDefaultStudyForm();
  }

  private StudyConfig createDefaultStudyForm() {
    StudyConfig form = new StudyConfig();
    form.setDefinition(getDefaultStudyFormResourceAsString("definition.json"));
    form.setSchema(getDefaultStudyFormResourceAsString("schema.json"));
    form.setPopulationDefinition(getDefaultStudyFormResourceAsString("populationDefinition.json"));
    form.setPopulationSchema(getDefaultStudyFormResourceAsString("populationSchema.json"));
    form.setDceDefinition(getDefaultStudyFormResourceAsString("dceDefinition.json"));
    form.setDceSchema(getDefaultStudyFormResourceAsString("dceSchema.json"));
    return form;
  }

  private Resource getDefaultStudyFormResource(String name) {
    return new DefaultResourceLoader().getResource("classpath:config/study-form/" + name);
  }

  private String getDefaultStudyFormResourceAsString(String name) {
    try(Scanner s = new Scanner(getDefaultStudyFormResource(name).getInputStream())) {
      return s.useDelimiter("\\A").hasNext() ? s.next() : "";
    } catch(IOException e) {
      return "";
    }
  }
}
