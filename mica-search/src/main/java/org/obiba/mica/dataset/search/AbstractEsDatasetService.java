/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search;

import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.service.EsDatasetService;
import org.obiba.mica.search.AbstractIdentifiedDocumentService;

public abstract class AbstractEsDatasetService<T extends Dataset> extends AbstractIdentifiedDocumentService<T> implements EsDatasetService {

  @Override
  public long getStudiesWithVariablesCount() {
    return searcher.countDocumentsWithField(getIndexName(), getType(), "studyTable.studyId");
  }

}
