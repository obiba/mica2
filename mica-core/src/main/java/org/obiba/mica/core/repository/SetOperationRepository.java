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

import org.obiba.mica.core.domain.ComposedSet;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.domain.SetOperation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;


public interface SetOperationRepository extends MongoRepository<SetOperation, String> {

  /**
   * Find all composed sets that are part of a given operation for a type.
   *
   * @param type
   * @param id
   * @return
   */
  SetOperation findByTypeAndId(String type, String id);

}
