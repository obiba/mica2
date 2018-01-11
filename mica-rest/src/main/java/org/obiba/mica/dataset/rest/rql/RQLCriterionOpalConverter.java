/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest.rql;

import com.google.common.base.Strings;
import net.jazdw.rql.parser.ASTNode;

public class RQLCriterionOpalConverter {

  private String function;

  private boolean not = false;

  private String value;

  private RQLFieldReferences variableReferences;

  private RQLCriterionOpalConverter() {}

  private String getQuery(String field) {
    String query = function + "(" + field;
    if (Strings.isNullOrEmpty(value))
      query = query + ")";
    else
      query = query + ",(" + value + "))";
    return not ? "not(" + query + ")" : query;
  }

  public String getOpalQuery() {
    return getQuery(variableReferences.getOpalVariableReference());
  }

  public String getMicaQuery() {
    return getQuery(variableReferences.getMicaVariableReference());
  }

  public RQLFieldReferences getVariableReferences() {
    return variableReferences;
  }

  public static Builder newBuilder(ASTNode node) {
    return new Builder(node);
  }

  public static class Builder {

    private final RQLCriterionOpalConverter criterion;

    public Builder(ASTNode node) {
      criterion = new RQLCriterionOpalConverter();
      criterion.function = node.getName();
    }

    public Builder references(RQLFieldReferences variableReferences) {
      criterion.variableReferences = variableReferences;
      return this;
    }
    public Builder value(String value) {
      criterion.value = value;
      return this;
    }

    public Builder not(boolean not) {
      criterion.not = not;
      return this;
    }

    public RQLCriterionOpalConverter build() {
      return criterion;
    }

  }

}
