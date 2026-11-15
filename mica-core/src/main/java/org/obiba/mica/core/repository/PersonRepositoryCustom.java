/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
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

/**
 * Targeted writes: each one changes only what it is about, never the whole person document.
 */
public interface PersonRepositoryCustom {

  /**
   * Replaces the details (title, names, academic level, email, phone, institution) when the stored version is
   * {@code version} (0 matches a document without version), and increments the version.
   *
   * @return false when no document matched (missing or changed meanwhile)
   */
  boolean updateDetails(String id, long version, Person details);

  /** @return false when the person does not exist */
  boolean addStudyMembership(String id, String studyId, String role);

  /** @return false when the person does not exist */
  boolean removeStudyMembership(String id, String studyId, String role);

  /** @return false when the person does not exist */
  boolean addNetworkMembership(String id, String networkId, String role);

  /** @return false when the person does not exist */
  boolean removeNetworkMembership(String id, String networkId, String role);

  /** Removes all the memberships of the study, returns the ids of the persons that had one. */
  List<String> removeAllStudyMemberships(String studyId);

  /** Removes all the memberships of the network, returns the ids of the persons that had one. */
  List<String> removeAllNetworkMemberships(String networkId);

  /** Removes all the study and network memberships with this role, returns the ids of the persons that had one. */
  List<String> removeAllMembershipsWithRole(String role);

  /**
   * Replaces the whole document (reinstate) when the stored version is {@code version}, and increments the
   * version.
   *
   * @return false when no document matched
   */
  boolean replace(Person person, long version);

  /**
   * Deletes the person when it has not been written since it was read (same version and last modified date, every
   * write sets the date).
   *
   * @return false when the person changed meanwhile (or is missing), nothing deleted
   */
  boolean deleteIfUnchanged(Person read);

  /** The persons with this email, trimmed and ignoring case; empty email finds nobody. */
  List<Person> findByEmailIgnoreCase(String email);
}
