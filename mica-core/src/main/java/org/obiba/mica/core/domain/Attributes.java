/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.domain;

import java.io.Serializable;
import java.util.List;
import java.util.TreeMap;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;

public class Attributes extends TreeMap<String, LocalizedString> implements AttributeAware, Serializable {

  private static final long serialVersionUID = -2442237574490031195L;

  public void addAttribute(String name, @Nullable String namespace, LocalizedString values) {
    String key = Attribute.getMapKey(name, namespace);
    if(!containsKey(key)) {
      put(key, values);
    } else {
      get(key).merge(values);
    }
  }

  @Override
  public void addAttribute(Attribute attribute) {
    if(attribute == null) return;
    addAttribute(attribute.getName(), attribute.getNamespace(), attribute.getValues());
  }

  public void removeAttribute(String name, @Nullable String namespace) {
    String key = Attribute.getMapKey(name, namespace);
    remove(key);
  }

  @Override
  public void removeAttribute(Attribute attribute) {
    remove(attribute.getName(), attribute.getNamespace());
  }

  @Override
  public void removeAllAttributes() {
    clear();
  }

  @Override
  public boolean hasAttribute(String attName, @Nullable String namespace) {
    return containsKey(Attribute.getMapKey(attName, namespace));
  }

  @JsonIgnore
  public Attribute getAttribute(String attName, @Nullable String namespace) {
    if(!hasAttribute(attName, namespace)) throw new NoSuchAttributeException(attName, getClass().getName());
    LocalizedString values = get(Attribute.getMapKey(attName, namespace));
    return Attribute.Builder.newAttribute(attName).namespace(namespace).values(values).build();
  }

  public List<Attribute> asAttributeList() {
    List<Attribute> attributes = Lists.newArrayList();
    entrySet().forEach(entry -> attributes.add(Attribute.Builder.newAttribute(entry.getKey()).values(entry.getValue()).build()));
    return attributes;
  }

}
