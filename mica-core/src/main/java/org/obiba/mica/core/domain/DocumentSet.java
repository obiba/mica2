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


import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Set;

/**
 * Defines a set of documents by specifying their type and a list of identifiers.
 */
@Document
public class DocumentSet extends DefaultEntityBase {

  private String type;

  private String name;

  private String username;

  private Set<String> identifiers;

  private boolean locked = false;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean hasName() {
    return !Strings.isNullOrEmpty(name);
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public boolean hasUsername() {
    return !Strings.isNullOrEmpty(username);
  }

  public Set<String> getIdentifiers() {
    return identifiers == null ? identifiers = Sets.newLinkedHashSet() : identifiers;
  }

  public void setIdentifiers(List<String> identifiers) {
    this.identifiers = Sets.newLinkedHashSet(identifiers);
  }

  public void addAllIdentifiers(List<String> identifiers) {
    getIdentifiers().addAll(identifiers);
  }

  public void setLocked(boolean locked) {
    this.locked = locked;
  }

  public boolean isLocked() {
    return locked;
  }
}
