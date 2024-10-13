/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.network;

import java.util.List;

import org.obiba.mica.core.repository.DocumentRepository;
import org.obiba.mica.network.domain.Network;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

/**
 * Spring Data MongoDB repository for the {@link org.obiba.mica.network.domain.Network} entity.
 */
public interface NetworkRepository extends MongoRepository<Network, String>, NetworkRepositoryCustom, DocumentRepository<Network> {

  List<Network> findByStudyIds(String studyId);

  List<Network> findByNetworkIds(String networkId);

  @Query("{'model' : { $exists : false }}")
  List<Network> findWithoutModel();

  @Query(value = "{}", fields = "{_id : 1}")
  List<Network> findAllExistingIds();

}
