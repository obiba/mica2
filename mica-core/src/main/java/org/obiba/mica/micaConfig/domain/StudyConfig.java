package org.obiba.mica.micaConfig.domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class StudyConfig extends EntityConfig {

  private String populationSchema;

  private String dceSchema;

  private String populationDefinition;

  private String dceDefinition;

  @Override
  public String pathPrefix() {
    return "study-config";
  }

  @Override
  public Map<String, Serializable> parts() {
    final StudyConfig self = this;

    return new HashMap<String, Serializable>(){
      {
        put(self.getClass().getSimpleName(), self);
        put("definition", self.getDefinition());
        put("schema", self.getSchema());
        put("populationDefinition", self.getPopulationDefinition());
        put("populationSchema", self.getPopulationSchema());
        put("dceDefinition", self.getDceDefinition());
        put("dceSchema", self.getDceSchema());
      }
    };
  }

  public String getPopulationSchema() {
    return populationSchema;
  }

  public void setPopulationSchema(String populationSchema) {
    this.populationSchema = populationSchema;
  }

  public String getDceSchema() {
    return dceSchema;
  }

  public void setDceSchema(String dceSchema) {
    this.dceSchema = dceSchema;
  }

  public String getPopulationDefinition() {
    return populationDefinition;
  }

  public void setPopulationDefinition(String populationDefinition) {
    this.populationDefinition = populationDefinition;
  }

  public String getDceDefinition() {
    return dceDefinition;
  }

  public void setDceDefinition(String dceDefinition) {
    this.dceDefinition = dceDefinition;
  }
}
