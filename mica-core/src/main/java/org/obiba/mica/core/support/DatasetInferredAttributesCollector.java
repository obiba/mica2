package org.obiba.mica.core.support;

import org.obiba.mica.core.domain.Attribute;
import org.obiba.mica.dataset.domain.DatasetVariable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DatasetInferredAttributesCollector {
  Set<Attribute> keys = new HashSet<>();

  public void collect(DatasetVariable variable) {
    variable.getAttributes().asAttributeList().stream()
      .filter(Attribute::hasNamespace) // TODO filter from the config
      .forEach(attribute -> keys.add(attribute));
  }

  public Set<Attribute> getAttributes() {
    return Collections.unmodifiableSet(keys);
  }
}
