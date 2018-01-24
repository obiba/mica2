/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.project;

import org.obiba.mica.project.domain.Project;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

/**
 * Spring Data MongoDB repository for the {@link Project} entity.
 */
public interface ProjectRepository extends MongoRepository<Project, String> {


  @Query(value = "{}", fields = "{_id : 1}")
  List<Project> findAllExistingIds();

}
