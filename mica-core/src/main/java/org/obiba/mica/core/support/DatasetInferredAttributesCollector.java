package org.obiba.mica.core.support;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import org.obiba.mica.core.domain.Attribute;
import org.obiba.mica.dataset.domain.DatasetVariable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DatasetInferredAttributesCollector {
  private Set<Attribute> keys = new HashSet<>();

  private List<String> acceptedTaxonomies = new ArrayList<>();

  private Function<Attribute, Boolean> attributeFilterFunction = (Attribute attribute) -> attribute.hasNamespace();

  public DatasetInferredAttributesCollector(List<String> acceptedTaxonomies) {
    if (acceptedTaxonomies != null) this.acceptedTaxonomies = acceptedTaxonomies;

    if (!this.acceptedTaxonomies.isEmpty()) {
      attributeFilterFunction =
        (Attribute attribute) -> this.acceptedTaxonomies.contains(Strings.nullToEmpty(attribute.getNamespace()));
    }
  }

  private boolean filterByAcceptedTaxonomies(Attribute attribute) {
    return acceptedTaxonomies.contains(Strings.nullToEmpty(attribute.getNamespace()));
  }

  public void collect(DatasetVariable variable) {
    variable.getAttributes().asAttributeList().stream()
      .filter(attribute -> attributeFilterFunction.apply(attribute))
      .forEach(attribute -> keys.add(attribute));
  }

  public Set<Attribute> getAttributes() {
    return Collections.unmodifiableSet(keys);
  }
}
