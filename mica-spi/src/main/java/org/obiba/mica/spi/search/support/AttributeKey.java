/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.spi.search.support;

import java.io.Serializable;
import java.util.Objects;

import jakarta.annotation.Nullable;

import com.google.common.base.Strings;
import jakarta.validation.constraints.NotNull;

public class AttributeKey implements Serializable {

  private static final long serialVersionUID = -3129195422457806471L;

  private static String SEPARATOR = "__";

  @NotNull
  private String name;

  private String namespace;

  public AttributeKey(@NotNull String name, @Nullable String namespace) {
    this.name = name;
    this.namespace = namespace;
  }

  @NotNull
  public String getName() {
    return name;
  }

  @Nullable
  public String getNamespace() {
    return namespace;
  }

  public boolean hasNamespace() {
    return !Strings.isNullOrEmpty(namespace);
  }

  public boolean hasNamespace(@Nullable String namespace) {
    return namespace == null ? this.namespace == null : namespace.equals(this.namespace);
  }

  public static String getMapKey(String name, @Nullable String namespace) {
    return Strings.isNullOrEmpty(namespace) ? name : namespace + SEPARATOR + name;
  }

  public static AttributeKey from(String mapKey) {
    if (!mapKey.contains(SEPARATOR)) return new AttributeKey(mapKey, null);
    else {
      String namespace = mapKey.substring(0, mapKey.indexOf(SEPARATOR));
      return new AttributeKey(mapKey.substring(mapKey.indexOf(SEPARATOR) + 2), namespace);
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, namespace);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) return true;
    if(obj == null || getClass() != obj.getClass()) return false;
    AttributeKey other = (AttributeKey) obj;
    return Objects.equals(name, other.name) && Objects.equals(namespace, other.namespace);
  }

  @Override
  public String toString() {
    return getMapKey(name, namespace);
  }

}
