/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.queries;

public interface JoinQueryWrapper {

  String DEFAULT_LOCALE = "en";

  boolean isWithFacets();

  String getLocale();

  QueryWrapper getVariableQueryWrapper();

  QueryWrapper getDatasetQueryWrapper();

  QueryWrapper getStudyQueryWrapper();

  QueryWrapper getNetworkQueryWrapper();

  default boolean searchOnNetworksOnly() {
    return false;
  }
}
