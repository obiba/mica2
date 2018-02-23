/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.repository;

import org.obiba.mica.core.domain.DocumentSet;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;


public interface DocumentSetRepository extends MongoRepository<DocumentSet, String> {

  /**
   * Find document sets of a given type.
   *
   * @param type
   * @return
   */
  List<DocumentSet> findByType(String type);

  /**
   * Find document sets of a given type and user name.
   *
   * @param type
   * @param username
   * @return
   */
  List<DocumentSet> findByTypeAndUsername(String type, String username);

}
