/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
 *
 *  This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.taxonomy;

import java.io.IOException;

import org.elasticsearch.search.SearchHit;
import org.obiba.mica.search.AbstractDocumentService;
import org.springframework.stereotype.Service;

@Service
public class EsTaxonomyTermService extends AbstractDocumentService<String> {

  @Override
  protected String processHit(SearchHit hit) throws IOException {
    return hit.getId();
  }

  @Override
  protected String getIndexName() {
    return TaxonomyIndexer.TAXONOMY_INDEX;
  }

  @Override
  protected String getType() {
    return TaxonomyIndexer.TAXONOMY_TERM_TYPE;
  }
}
