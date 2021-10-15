/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.domain;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.hibernate.validator.constraints.URL;
import org.obiba.mica.core.domain.*;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.spi.search.Indexable;

import javax.validation.constraints.NotNull;
import java.beans.Transient;
import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * Base class for representing all type of studies.
 */
public abstract class BaseStudy extends AbstractModelAware implements PersonAware, Indexable {

  private Attachment logo;

  public final static String MAPPING_NAME = "Study";

  @NotNull
  private LocalizedString name;

  private LocalizedString acronym;

  private LocalizedString objectives;

  private Map<String, List<String>> membershipSortOrder = new HashMap<>();

  private Map<String, List<Membership>> memberships = new HashMap<String, List<Membership>>() {
    {
      put(Membership.CONTACT, Lists.newArrayList());
      put(Membership.INVESTIGATOR, Lists.newArrayList());
    }
  };

  @URL
  private String opal;

  private Set<String> sets;

  private SortedSet<Population> populations = Sets.newTreeSet();

  //
  // Accessors
  //

  public abstract String getResourcePath();

  public Attachment getLogo() {
    return logo;
  }

  public boolean hasLogo() {
    return logo != null;
  }

  public void setLogo(Attachment logo) {
    this.logo = logo;
  }

  public LocalizedString getName() {
    return name;
  }

  public void setName(LocalizedString name) {
    this.name = name;
  }

  public LocalizedString getAcronym() {
    return acronym;
  }

  public void setAcronym(LocalizedString acronym) {
    this.acronym = acronym;
  }

  public LocalizedString getObjectives() {
    return objectives;
  }

  public void setObjectives(LocalizedString objectives) {
    this.objectives = objectives;
  }

  public String getOpal() {
    return opal;
  }

  public void setOpal(String opal) {
    this.opal = opal;
  }

  public Set<String> getSets() {
    return sets;
  }

  public void setSets(Set<String> sets) {
    this.sets = sets;
  }

  public void addSet(String id) {
    if (sets == null) {
      sets = Sets.newLinkedHashSet();
    }
    sets.add(id);
  }

  public void removeSet(String id) {
    if (sets == null) return;
    sets.remove(id);
  }

  public boolean containsSet(String id) {
    if (sets == null) return false;
    return sets.contains(id);
  }

  public Map<String, List<String>> getMembershipSortOrder() {
    if (membershipSortOrder == null) return new HashMap<>();
    return membershipSortOrder;
  }

  public void setMembershipSortOrder(Map<String, List<String>> membershipSortOrder) {
    this.membershipSortOrder = membershipSortOrder;
  }

  //
  // PersonAware methods
  //

  public Set<String> membershipRoles() {
    return this.memberships.keySet();
  }

  public Map<String, List<Membership>> getMemberships() {
    return memberships;
  }

  @Override
  public List<Person> getAllPersons() {
    return getMemberships().values().stream().flatMap(List::stream).map(Membership::getPerson).distinct()
      .collect(toList());
  }

  @Override
  public List<Membership> getAllMemberships() {
    return getMemberships().values().stream().flatMap(List::stream).collect(toList());
  }

  @Override
  public void addToPerson(Membership membership) {
    membership.getPerson().addStudy(this, membership.getRole());
  }

  @Override
  public void removeFromPerson(Membership membership) {
    membership.getPerson().removeStudy(this, membership.getRole());
  }

  @Override
  public void removeFromPerson(Person person) {
    person.removeStudy(this);
  }

  public void setMemberships(Map<String, List<Membership>> memberships) {
    if (memberships == null) {
      this.memberships.clear();
    } else {
      this.memberships = memberships;
    }
  }

  public List<Person> removeRole(String role) {
    List<Membership> members = this.memberships.getOrDefault(role, Lists.newArrayList());
    this.memberships.remove(role);
    return members.stream().map(m -> {
      m.getPerson().removeStudy(this, role);
      return m.getPerson();
    }).collect(toList());
  }

  public SortedSet<Population> getPopulations() {
    return populations;
  }

  @Transient
  public List<Population> getPopulationsSorted() {
    return populations.stream().sorted(Comparator.comparing(Population::getWeight))
      .collect(toList());
  }

  public void addPopulation(@NotNull Population population) {
    if (populations == null) populations = new TreeSet<>();
    if (population.isNew()) {
      String newId = population.getName().asAcronym().asUrlSafeString().toLowerCase();
      if (hasPopulation(newId)) {
        for (int i = 1; i < 1000; i++) {
          if (!hasPopulation(newId + "_" + i)) {
            population.setId(newId + "_" + i);
            break;
          }
        }
      } else population.setId(newId);
    }
    populations.add(population);
  }

  public boolean hasPopulation(String populationId) {
    if (populations == null) return false;
    for (Population population : populations) {
      if (population.getId().equals(populationId)) return true;
    }
    return false;
  }

  public boolean hasPopulations() {
    return populations != null && !populations.isEmpty();
  }

  public void setPopulations(SortedSet<Population> newPopulations) {
    if (newPopulations == null) {
      // during serialization input can be null
      populations = null;
      return;
    }

    // make sure we don't keep old entries
    populations = new TreeSet<>();
    newPopulations.forEach(this::addPopulation);
  }

  public Population findPopulation(String id) {
    return populations.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
  }

  //
  // Index
  //

  public String getClassName() {
    return getClass().getSimpleName();
  }

  @Override
  public String getMappingName() {
    return MAPPING_NAME;
  }

  @Override
  public String getParentId() {
    return null;
  }

  // for JSON deserial
  public void setClassName(String className) {}


}
