package org.obiba.mica.project.domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.obiba.mica.JSONUtils;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.core.domain.LocalizedString;

import com.google.common.base.Strings;

public class Project extends AbstractGitPersistable {

  private LocalizedString name;

  private LocalizedString description;

  private Map<String,Object> content;

  private String dataAccessRequestId;

  public Project() {}

  //
  // Accessors
  //

  public boolean hasName() {
    return name != null;
  }

  public LocalizedString getName() {
    return name;
  }

  public void setName(LocalizedString name) {
    this.name = name;
  }

  public boolean hasContent() {
    return content != null;
  }

  public void setContent(Map<String, Object> content) {
    this.content = content;
  }

  public Map<String, Object> getContent() {
    return content;
  }

  public boolean hasDataAccessRequestId() {
    return !Strings.isNullOrEmpty(dataAccessRequestId);
  }

  public void setDataAccessRequestId(String dataAccessRequestId) {
    this.dataAccessRequestId = dataAccessRequestId;
  }

  public String getDataAccessRequestId() {
    return dataAccessRequestId;
  }

  @Override
  public String pathPrefix() {
    return "projects";
  }

  @Override
  public Map<String, Serializable> parts() {
    Project self = this;

    return new HashMap<String, Serializable>() {
      {
        put(self.getClass().getSimpleName(), self);
      }
    };
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public LocalizedString getDescription() {
    return description;
  }

  public void setDescription(LocalizedString description) {
    this.description = description;
  }

  public static class Builder {
    private Project project;

    Builder() {
      project = new Project();
    }

    public Builder content(String content) {
      if (Strings.isNullOrEmpty(content)) {
        project.content = null;
      } else {
        project.content = JSONUtils.toMap(content);
      }
      return this;
    }

    public Builder name(LocalizedString value) {
      project.setName(value);
      return this;
    }

    public Builder description(LocalizedString value) {
      project.setDescription(value);
      return this;
    }

    public Project build() {
      return project;
    }
  }
}
