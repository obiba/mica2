/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest.variable;

public class DatasetVariableResource {

  private String datasetId;

  private String name;

  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
