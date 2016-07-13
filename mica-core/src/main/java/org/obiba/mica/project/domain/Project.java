package org.obiba.mica.project.domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.obiba.mica.JSONUtils;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.core.domain.LocalizedString;

import com.google.common.base.Strings;

public class Project extends AbstractGitPersistable {

  private LocalizedString title;

  private LocalizedString summary;

  private Map<String,Object> model;

  private String dataAccessRequestId;

  public Project() {}

  //
  // Accessors
  //

  public boolean hasTitle() {
    return title != null;
  }

  public LocalizedString getTitle() {
    return title;
  }

  public void setTitle(LocalizedString title) {
    this.title = title;
  }

  public boolean hasModel() {
    return model != null;
  }

  public void setModel(Map<String, Object> model) {
    this.model = model;
  }

  public Map<String, Object> getModel() {
    return model;
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

  public LocalizedString getSummary() {
    return summary;
  }

  public void setSummary(LocalizedString summary) {
    this.summary = summary;
  }

  public static class Builder {
    private Project project;

    Builder() {
      project = new Project();
    }

    public Builder content(String content) {
      if (Strings.isNullOrEmpty(content)) {
        project.model = null;
      } else {
        project.model = JSONUtils.toMap(content);
      }
      return this;
    }

    public Builder title(LocalizedString value) {
      project.setTitle(value);
      return this;
    }

    public Builder summary(LocalizedString value) {
      project.setSummary(value);
      return this;
    }

    public Project build() {
      return project;
    }
  }
}
