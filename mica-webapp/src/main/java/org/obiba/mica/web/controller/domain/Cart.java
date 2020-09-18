package org.obiba.mica.web.controller.domain;

import org.obiba.mica.core.domain.DocumentSet;

public class Cart {

  private final DocumentSet set;

  public Cart(DocumentSet set) {
    this.set = set;
  }

  public String getId() {
    return set.getId();
  }

  public int getCount() {
    return set.getIdentifiers().size();
  }

  public DocumentSet getSet() {
    return set;
  }
}
