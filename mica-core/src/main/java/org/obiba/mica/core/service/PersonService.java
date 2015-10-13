/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.service;

import java.util.List;

import javax.inject.Inject;

import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class PersonService {

  @Inject
  PersonRepository personRepository;

  public Person findById(String id) {
    return personRepository.findOne(id);
  }

  public List<Person> find(String query) {
    return null;
  }

  public List<Person> findAllPersons() {
    return null;
  }
}
