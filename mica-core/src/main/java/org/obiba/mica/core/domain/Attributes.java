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

import java.io.Serializable;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import org.obiba.mica.spi.search.support.AttributeKey;

public class Attributes extends TreeMap<String, LocalizedString> implements AttributeAware, Serializable {

  private static final long serialVersionUID = -2442237574490031195L;

  public void addAttribute(String name, @Nullable String namespace, LocalizedString values) {
    AttributeKey key = new AttributeKey(name, namespace);
    if(!containsKey(key.toString())) {
      put(key.toString(), values);
    } else {
      get(key.toString()).merge(values);
    }
  }

  @Override
  public void addAttribute(Attribute attribute) {
    if(attribute == null) return;
    addAttribute(attribute.getName(), attribute.getNamespace(), attribute.getValues());
  }

  public void removeAttribute(String name, @Nullable String namespace) {
    String key = AttributeKey.getMapKey(name, namespace);
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
    return containsKey(AttributeKey.getMapKey(attName, namespace));
  }

  @JsonIgnore
  public Attribute getAttribute(String attName, @Nullable String namespace) {
    if(!hasAttribute(attName, namespace)) throw new NoSuchAttributeException(attName, getClass().getName());
    LocalizedString values = get(AttributeKey.getMapKey(attName, namespace));
    return Attribute.Builder.newAttribute(attName).namespace(namespace).values(values).build();
  }

  @JsonIgnore
  public List<Attribute> getAttributes(@Nullable String namespace) {
    return entrySet().stream().filter(e -> AttributeKey.from(e.getKey()).hasNamespace(namespace))
        .map(e -> Attribute.Builder.newAttribute(e.getKey()).values(e.getValue()).build()).collect(Collectors.toList());
  }

  public List<Attribute> asAttributeList() {
    List<Attribute> attributes = Lists.newArrayList();
    entrySet().forEach(
        entry -> attributes.add(Attribute.Builder.newAttribute(entry.getKey()).values(entry.getValue()).build()));
    return attributes;
  }

}
