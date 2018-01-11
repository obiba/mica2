/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.security.repository;

import java.util.List;

import org.obiba.mica.security.domain.SubjectAcl;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface SubjectAclRepository extends MongoRepository<SubjectAcl, String> {

  List<SubjectAcl> findByPrincipalAndType(String principal, SubjectAcl.Type type);

  @Query("{'resource': ?0, 'instance': {$regex: ?1}}")
  List<SubjectAcl> findByResourceAndInstanceRegex(String resource, String instance);

  List<SubjectAcl> findByResourceAndInstance(String resource, String instance);

  List<SubjectAcl> findByResourceAndInstance(String resource, String instance, Sort sort);

  List<SubjectAcl> findByResourceStartingWith(String regex);

  List<SubjectAcl> findByPrincipalAndTypeAndResourceAndInstance(String principal, SubjectAcl.Type type, String resource, String instance);

}
