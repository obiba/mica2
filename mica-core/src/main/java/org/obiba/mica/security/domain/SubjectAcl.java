/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.security.domain;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import org.obiba.core.util.StringUtil;
import org.obiba.mica.core.domain.AbstractAuditableDocument;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;

@Document
public class SubjectAcl extends AbstractAuditableDocument implements Comparable<SubjectAcl> {

  private static final long serialVersionUID = -3689274856936068056L;

  protected static final String WILDCARD_TOKEN = "*";

  protected static final String PART_DIVIDER_TOKEN = ":";

  protected static final String SUBPART_DIVIDER_TOKEN = ",";

  private String principal;

  private Type type;

  private String resource;

  private List<String> actions;

  private String instance;

  public String getPrincipal() {
    return principal;
  }

  public void setPrincipal(String principal) {
    this.principal = principal;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public String getResource() {
    return resource;
  }

  public void setResource(String resource) {
    this.resource = resource;
  }

  public List<String> getActions() {
    return actions == null ? actions = Lists.newArrayList() : actions;
  }

  public void setActions(List<String> actions) {
    this.actions = actions;
  }

  public boolean hasActions() {
    return !getActions().isEmpty();
  }

  /**
   * Check if is one the actions. Several actions can be provided, comma-separated.
   *
   * @param action
   * @return
   */
  public boolean hasAction(String action) {
    for(String a : action.split(SUBPART_DIVIDER_TOKEN)) {
      if(getActions().contains(a)) return true;
    }
    return false;
  }

  /**
   * Add an action (if not already done). Can be several actions, comma-separated.
   *
   * @param action
   */
  public void addAction(String action) {
    Stream.of(action.split(SUBPART_DIVIDER_TOKEN)).forEach(a -> {
      if(!getActions().contains(a)) {
        getActions().add(a);
      }
    });
  }

  /**
   * Remove an action. Several actions can be provided, comma-separated.
   *
   * @param action
   */
  public void removeAction(String action) {
    Stream.of(action.split(SUBPART_DIVIDER_TOKEN)).forEach(a -> getActions().remove(a));
  }

  /**
   * Remove all actions.
   */
  public void removeActions() {
    actions = null;
  }

  public String getInstance() {
    return instance;
  }

  public void setInstance(String instance) {
    this.instance = instance;
  }

  /**
   * Get string representation of the permission compatible with {@link org.apache.shiro.authz.permission.WildcardPermission}.
   *
   * @return
   */
  @JsonIgnore
  public String getPermission() {
    StringBuilder builder = new StringBuilder();
    if(Strings.isNullOrEmpty(resource)) builder.append(WILDCARD_TOKEN);
    else builder.append(resource);
    builder.append(PART_DIVIDER_TOKEN);
    String actionsStr = StringUtil.collectionToString(actions, SUBPART_DIVIDER_TOKEN);
    if(Strings.isNullOrEmpty(actionsStr)) builder.append(WILDCARD_TOKEN);
    else builder.append(actionsStr);
    builder.append(PART_DIVIDER_TOKEN);
    if(Strings.isNullOrEmpty(instance)) builder.append(WILDCARD_TOKEN);
    else builder.append(instance);
    return builder.toString();
  }

  @Override
  public int compareTo(SubjectAcl o) {
    if (equals(o)) return 0;
    int cmp = getType().toString().compareTo(o.getType().toString());
    if (cmp != 0) return cmp;
    cmp = getPrincipal().compareToIgnoreCase(o.getPrincipal());
    if (cmp != 0) return cmp;
    return getResource().compareTo(o.getResource());
  }

  //
  // Inner classes and enum
  //

  public static Builder newBuilder(String name, Type type) {
    return new Builder(name, type);
  }


  public enum Type {
    USER, GROUP;

    public Subject subjectFor(@SuppressWarnings("ParameterHidesMemberVariable") String principal) {
      return new Subject(principal, this);
    }
  }

  public static class Builder {

    private final SubjectAcl subjectAcl = new SubjectAcl();

    private final List<String> actions = Lists.newArrayList();

    public Builder(String name, Type type) {
      subjectAcl.principal = name;
      subjectAcl.type = type;
    }

    public Builder resource(String resource) {
      subjectAcl.resource = resource;
      return this;
    }

    public Builder action(String... action) {
      if(action != null) {
        Stream.of(action).forEach(a -> {
          Stream.of(a.split(SUBPART_DIVIDER_TOKEN)).forEach(actions::add);
        });
      }
      return this;
    }

    public Builder instance(String instance) {
      subjectAcl.instance = instance;
      return this;
    }

    public SubjectAcl build() {
      subjectAcl.setActions(actions);
      return subjectAcl;
    }

  }

  public static class Subject implements Comparable<Subject>, Serializable {

    private static final long serialVersionUID = -4104563748622536925L;

    private final String principal;

    private final Type type;

    public Subject(String principal, Type type) {
      this.principal = principal;
      this.type = type;
    }

    public String getPrincipal() {
      return principal;
    }

    public Type getType() {
      return type;
    }

    @Override
    public int compareTo(@NotNull Subject other) {
      return ComparisonChain.start() //
        .compare(type, other.type) //
        .compare(principal, other.principal) //
        .result();
    }

    @Override
    public String toString() {
      return getType() + ":" + getPrincipal();
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(principal, type);
    }

    @Override
    public boolean equals(Object obj) {
      if(this == obj) return true;
      if(obj == null || getClass() != obj.getClass()) return false;
      Subject other = (Subject) obj;
      return Objects.equal(principal, other.principal) && Objects.equal(type, other.type);
    }
  }

}
