/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search;

import java.util.List;

import com.google.common.collect.Lists;

public class DatasetIdProvider {

  private List<String> datasetIds = Lists.newArrayList();

  public void setDatasetIds(List<String> ids) {
    if (ids != null) datasetIds = ids;
  }

  public List<String> getDatasetIds() {
    return datasetIds;
  }

  public void resetDatasetIds() {
    datasetIds.clear();
  }

}
