/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.taxonomy;

import org.obiba.mica.search.AbstractDocumentService;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EsTaxonomyTermService extends AbstractDocumentService<String> {

  @Override
  protected String processHit(Searcher.DocumentResult res) throws IOException {
    return res.getId();
  }

  @Override
  protected String getIndexName() {
    return Indexer.TAXONOMY_INDEX;
  }

  @Override
  protected String getType() {
    return Indexer.TAXONOMY_TERM_TYPE;
  }
}
