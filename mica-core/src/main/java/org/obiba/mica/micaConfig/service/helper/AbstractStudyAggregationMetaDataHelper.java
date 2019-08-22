/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service.helper;

import org.obiba.opal.core.domain.taxonomy.Term;

public abstract class AbstractStudyAggregationMetaDataHelper extends AbstractIdAggregationMetaDataHelper {

  @Override
  protected Term createTermFromMetaData(String id, AggregationMetaDataProvider.LocalizedMetaData metaData) {
    Term term = super.createTermFromMetaData(id, metaData);
    if(metaData.getStart() != null) term.addAttribute("start", metaData.getStart());
    if(metaData.getEnd() != null) term.addAttribute("end", metaData.getEnd());
    if (metaData.getSortField() != null) term.addAttribute("sortField", metaData.getSortField());
    return term;
  }

}
