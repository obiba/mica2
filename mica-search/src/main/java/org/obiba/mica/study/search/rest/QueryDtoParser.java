/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.search.rest;

import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import static org.obiba.mica.web.model.MicaSearch.BoolFilterQueryDto;
import static org.obiba.mica.web.model.MicaSearch.FilteredQueryDto;
import static org.obiba.mica.web.model.MicaSearch.QueryDto;

public class QueryDtoParser {

  private QueryDtoParser() {}

  public static QueryDtoParser newParser() {
    return new QueryDtoParser();
  }

  public FilteredQueryBuilder parse(QueryDto queryDto) {
    FilteredQueryDto filteredQueryDto = queryDto.getFilteredQuery();
    BoolFilterQueryDto boolFilterDto = filteredQueryDto.getFilter();
    BoolFilterBuilder boolFilter = FilterBuilders.boolFilter();

    if(boolFilterDto.getTermsList().size() > 0) {
      boolFilterDto.getTermsList()
          .forEach(terms -> boolFilter.must(FilterBuilders.termsFilter(terms.getField(), terms.getValuesList())));
    }

    return QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), boolFilter);
  }

}
