/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.source.support;

import com.google.common.collect.Lists;
import org.obiba.magma.type.BooleanType;
import org.obiba.mica.spi.tables.IVariable;
import org.obiba.opal.web.model.Search;

import java.util.List;

public class QueryTermsUtil {

  public static final String TOTAL_FACET = "_total";

  private QueryTermsUtil() {
  }

  public static Search.QueryTermsDto getContingencyQuery(IVariable variable, IVariable crossVariable) {
    Search.QueryTermsDto.Builder queries = Search.QueryTermsDto.newBuilder();

    // for each category, make a facet query
    getCategories(variable).stream().map(c -> getQueryTerm(variable, crossVariable, c))
      .forEach(queries::addQueries);
    // make a facet query for all categories
    queries.addQueries(getTotalTerm(variable, crossVariable));

    return queries.build();
  }

  //
  // Private methods
  //

  private static Search.QueryTermDto getQueryTerm(IVariable variable, IVariable crossVariable, String facetName) {
    Search.QueryTermDto.Builder query = Search.QueryTermDto.newBuilder();
    query.setFacet(facetName);

    query.setExtension(Search.VariableTermDto.field,
      Search.VariableTermDto.newBuilder().setVariable(crossVariable.getName()).build());
    query.setExtension(Search.LogicalTermDto.facetFilter, getLogicalTermDto(variable.getName(), facetName));

    return query.build();
  }

  private static Search.QueryTermDto getTotalTerm(IVariable variable, IVariable crossVariable) {
    Search.QueryTermDto.Builder query = Search.QueryTermDto.newBuilder();
    query.setFacet(TOTAL_FACET);

    query.setExtension(Search.VariableTermDto.field,
      Search.VariableTermDto.newBuilder().setVariable(crossVariable.getName()).build());

    query.setExtension(Search.LogicalTermDto.facetFilter, getLogicalTermDto(variable.getName(), getCategories(variable)));

    return query.build();
  }

  private static Search.LogicalTermDto getLogicalTermDto(String variableName, String categoryName) {
    return getLogicalTermDto(variableName, Lists.newArrayList(categoryName));
  }

  private static Search.LogicalTermDto getLogicalTermDto(String variableName, List<String> categories) {
    Search.LogicalTermDto.Builder term = Search.LogicalTermDto.newBuilder();
    term.setOperator(Search.TermOperator.AND_OP);
    Search.FilterDto.Builder filter = Search.FilterDto.newBuilder();
    filter.setVariable(variableName);
    Search.InTermDto.Builder inTerm = Search.InTermDto.newBuilder();
    inTerm.addAllValues(categories);
    inTerm.setMinimumMatch(1);
    filter.setExtension(Search.InTermDto.terms, inTerm.build());
    term.addExtension(Search.FilterDto.filters, filter.build());
    return term.build();
  }

  private static List<String> getCategories(IVariable variable) {
    List<String> categories = Lists.newArrayList();
    if (variable.getValueType().equals(BooleanType.get().getName())) {
      categories.add("true");
      categories.add("false");
    } else if (variable.hasCategories()) {
      categories = variable.getCategoryNames();
    }
    return categories;
  }
}
