package org.obiba.mica.micaConfig.service;

public class TaxonomyNotFoundException extends RuntimeException {
  private static final long serialVersionUID = 4661208635995765840L;

  private String taxonomyName;

  public TaxonomyNotFoundException() {
    super();
  }

  public TaxonomyNotFoundException(String tentativeTaxonomyName) {
    super(String.format("The taxonomy \"%s\" is not found.", tentativeTaxonomyName));
    taxonomyName = tentativeTaxonomyName;
  }

  public String getTaxonomyName() {
    return taxonomyName;
  }
}
