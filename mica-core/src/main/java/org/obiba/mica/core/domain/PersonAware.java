/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface PersonAware {
  @JsonIgnore
  List<Person> getAllPersons();

  @JsonIgnore
  List<Membership> getAllMemberships();

  void addToPerson(Membership membership);

  void removeFromPerson(Membership membership);

  void removeFromPerson(Person person);
}
