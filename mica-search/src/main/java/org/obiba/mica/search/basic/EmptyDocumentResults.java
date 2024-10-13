/*
 * Copyright (c) 2024 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.basic;

import org.obiba.mica.spi.search.Searcher;

import java.util.List;
import java.util.Map;

public class EmptyDocumentResults implements Searcher.DocumentResults {
  @Override
  public long getTotal() {
    return 0;
  }

  @Override
  public List<Searcher.DocumentResult> getDocuments() {
    return List.of();
  }

  @Override
  public Map<String, Long> getAggregation(String field) {
    return Map.of();
  }

  @Override
  public List<Searcher.DocumentAggregation> getAggregations() {
    return List.of();
  }
}
