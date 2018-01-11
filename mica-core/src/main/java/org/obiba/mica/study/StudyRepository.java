/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study;

import org.obiba.mica.study.domain.Study;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

/**
 * Spring Data MongoDB repository for the Study entity.
 */

public interface StudyRepository extends MongoRepository<Study, String>, StudyRepositoryCustom {

  @Query("{'model' : { $exists : false }}")
  List<Study> findWithoutModel();

  @Query(value = "{'_id' : { $in: ?0 }}", fields = "{_id : 1}")
  List<Study> findAllExistingIds(Iterable<String> ids);
}
