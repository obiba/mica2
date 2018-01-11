/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.domain;

import javax.validation.constraints.NotNull;

/**
 * Represents a table in opal.
 */
public abstract class OpalTable {

  @NotNull
  private String project;

  @NotNull
  private String table;

  private LocalizedString name;

  private LocalizedString description;

  private int weight;

  public String getProject() {
    return project;
  }

  public void setProject(String project) {
    this.project = project;
  }

  public String getTable() {
    return table;
  }

  public void setTable(String table) {
    this.table = table;
  }

  public void setName(LocalizedString name) {
    this.name = name;
  }

  public LocalizedString getName() {
    return name;
  }

  public void setDescription(LocalizedString description) {
    this.description = description;
  }

  public LocalizedString getDescription() {
    return description;
  }

  public int getWeight() {
    return weight;
  }

  public void setWeight(int weight) {
    this.weight = weight;
  }

  public boolean isFor(String entityId, String project, String table) {
    return getEntityId().equals(entityId) && getProject().equals(project) && getTable().equals(table);
  }

  protected abstract String getEntityId();
}
