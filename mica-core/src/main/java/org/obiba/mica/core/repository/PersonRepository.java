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

import java.util.List;

import org.obiba.mica.core.domain.Person;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface PersonRepository extends MongoRepository<Person, String>, DocumentRepository<Person> {

  @Query(value = "{'studyMemberships.parentId' : ?0 }")
  List<Person> findByStudyMemberships(String studyId);

  @Query(value = "{'networkMemberships.parentId' : ?0 }")
  List<Person> findByNetworkMemberships(String networkId);

  @Query(value = "{'studyMemberships.role' : ?0 }")
  List<Person> findByStudyMembershipsRole(String role);

  @Query(value = "{'networkMemberships.role' : ?0 }")
  List<Person> findByNetworkMembershipsRole(String role);

  Person findOneByEmail(String email);

  @Query("{$where: \"this.institution && this.institution.address && this.institution.address.countryIso && this.institution.address.countryIso.length == 2\"}")
  Iterable<Person> findAllWhenCountryIsoContainsTwoCharacters();
}
