/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.domain;

import javax.annotation.Nullable;

public abstract class AbstractAttributeAware implements AttributeAware {

  private Attributes attributes;

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
