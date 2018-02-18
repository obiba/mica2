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
import com.google.common.collect.Lists;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document
public class SetOperation extends DefaultEntityBase {

  private String type;

  private String name;

  private String username;

  private List<ComposedSet> compositions;

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

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public boolean hasUsername() {
    return !Strings.isNullOrEmpty(username);
  }

  public List<ComposedSet> getCompositions() {
    return compositions == null ? compositions = Lists.newArrayList() : compositions;
  }

  public void setCompositions(List<ComposedSet> compositions) {
    this.compositions = compositions;
  }

  public void addComposition(ComposedSet composition) {
    int pos = getCompositions().size() + 1;
    composition.setId("" + pos);
    getCompositions().add(composition);
  }
}
