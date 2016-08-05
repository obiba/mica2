package org.obiba.mica.core.domain;

import java.util.Map;

import javax.annotation.Nullable;

public abstract class AbstractAttributeModelAware implements AttributeAware, ModelAware {

  private Attributes attributes;

  private Map<String, Object> model;

  public boolean hasModel() {
    return model != null;
  }

  public void setModel(Map<String, Object> model) {
    this.model = model;
  }

  public Map<String, Object> getModel() {
    return model;
  }

  public Attributes getAttributes() {
    return attributes;
  }

  @Override
  public void addAttribute(Attribute attribute) {
    if(attributes == null) attributes = new Attributes();
    attributes.addAttribute(attribute);
  }

  @Override
  public void removeAttribute(Attribute attribute) {
    if(attributes != null) {
      attributes.removeAttribute(attribute);
    }
  }

  @Override
  public void removeAllAttributes() {
    if(attributes != null) attributes.removeAllAttributes();
  }

  @Override
  public boolean hasAttribute(String attName, @Nullable String namespace) {
    return attributes != null && attributes.hasAttribute(attName, namespace);
  }

}
