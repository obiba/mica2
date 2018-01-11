/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.project.domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.obiba.mica.JSONUtils;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.spi.search.Indexable;
import org.obiba.mica.core.domain.LocalizedString;

import com.google.common.base.Strings;

public class Project extends AbstractGitPersistable implements Indexable {

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

  @Override
  public String getClassName() {
    return getClass().getSimpleName();
  }

  @Override
  public String getMappingName() {
    return null;
  }

  @Override
  public String getParentId() {
    return getId();
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
