/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.service;

import org.obiba.mica.spi.search.Identified;

import java.util.List;

public interface IdentifiedDocumentService<T extends Identified> extends DocumentService<T> {

  /**
   * Get one document by its ID, returns null if not found.
   *
   * @param id
   * @return
   */
  T findById(String id);

  /**
   * List all documents matching a list of IDs.
   *
   * @param ids
   * @return
   */
  List<T> findByIds(List<String> ids);

  /**
   * Whether the document service implementation shall use cache. Note that cachable document are expected to
   * be instances of {@link org.obiba.mica.spi.search.Identified}.
   * @return
   */
  default boolean useCache() {
    return false;
  }

}
