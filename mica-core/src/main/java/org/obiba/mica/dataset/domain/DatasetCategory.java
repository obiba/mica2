/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.domain;

import javax.annotation.Nullable;

import org.obiba.magma.Category;
import org.obiba.mica.core.domain.Attribute;
import org.obiba.mica.core.domain.AttributeAware;
import org.obiba.mica.core.domain.Attributes;

public class DatasetCategory implements AttributeAware {

  private String name;

  private boolean missing;

  private Attributes attributes;

  public DatasetCategory() {}

  public DatasetCategory(Category category) {
    name = category.getName();
    missing = category.isMissing();
    if(category.hasAttributes()) {
      for(org.obiba.magma.Attribute attr : category.getAttributes()) {
        addAttribute(Attribute.Builder.newAttribute(attr).build());
      }
    }
  }

  public String getName() {
    return name;
  }

  public boolean isMissing() {
    return missing;
  }

  public boolean hasAttributes() {
    return attributes != null && !attributes.isEmpty();
  }

  public Attributes getAttributes() {
    return attributes;
  }

  @Override
  public void addAttribute(Attribute attribute) {
    if(attributes == null) attributes = new Attributes();
    attributes.addAttribute(attribute);
  }

  @Override
  public void removeAttribute(Attribute attribute) {
    if(attributes != null) {
      attributes.removeAttribute(attribute);
    }
  }

  @Override
  public void removeAllAttributes() {
    if(attributes != null) attributes.removeAllAttributes();
  }

  @Override
  public boolean hasAttribute(String attName, @Nullable String namespace) {
    return attributes != null && attributes.hasAttribute(attName, namespace);
  }

}
