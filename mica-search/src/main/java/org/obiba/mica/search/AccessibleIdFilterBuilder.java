package org.obiba.mica.search;

import com.google.common.collect.Lists;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.spi.search.Searcher;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class AccessibleIdFilterBuilder {

  private SubjectAclService service;
  private List<String> resources;
  private List<String> ids;
  private String action;

  private AccessibleIdFilterBuilder() {
  }

  public static AccessibleIdFilterBuilder newBuilder() {
    return new AccessibleIdFilterBuilder();
  }

  public AccessibleIdFilterBuilder aclService(SubjectAclService value) {
    this.service = value;
    return this;
  }

  public AccessibleIdFilterBuilder resources(List<String> value) {
    this.resources = value;
    return this;
  }

  public AccessibleIdFilterBuilder ids(List<String> value) {
    this.ids = value;
    return this;
  }

  public AccessibleIdFilterBuilder action(String value) {
    action = value;
    return this;
  }

  private List<String> findAuthorized(String resource) {
    return ids.stream()
      .filter(studyId -> service.isPermitted(resource, "VIEW", studyId))
      .collect(Collectors.toList());
  }

  public Searcher.IdFilter build() {
    List<String> authorizedIds = Lists.newArrayList();
    resources.forEach(resource -> authorizedIds.addAll(findAuthorized(resource)));

    return new Searcher.IdFilter() {
      @Override
      public Collection<String> getValues() {
        return authorizedIds;
      }
    };
  }

}
