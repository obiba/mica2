/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.queries;

import com.google.common.collect.Lists;

import java.util.List;

public class DocumentQueryJoinKeys {

  public final List<String> studyIds;
  public final List<String> datasetIds;

  public DocumentQueryJoinKeys() {
    studyIds = Lists.newArrayList();
    datasetIds = Lists.newArrayList();
  }

}
