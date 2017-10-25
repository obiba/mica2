package org.obiba.mica.study.domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class HarmonizationStudy extends BaseStudy {
  public static final String RESOURCE_PATH = "harmonization-study";

  private static final long serialVersionUID = -2711951694614982486L;

  @Override
  public String pathPrefix() {
    return "harmonizationStudies";
  }

  @Override
  public Map<String, Serializable> parts() {

    HarmonizationStudy self = this;

    return new HashMap<String, Serializable>() {
      {
        put(self.getClass().getSimpleName(), self);
      }
    };
  }

  @Override
  public String getResourcePath() {
    return RESOURCE_PATH;
  }
}
