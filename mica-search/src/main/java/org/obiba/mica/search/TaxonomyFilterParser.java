/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search;

import java.util.List;
import java.util.Map;

import org.obiba.mica.spi.search.support.AttributeKey;
import org.obiba.mica.web.model.MicaSearch;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Build the taxonomy terms from filtered queries.
 */
public class TaxonomyFilterParser {

  private final MicaSearch.QueryDto queryDto;

  private final Map<String, Map<String, List<String>>> termsMap = Maps.newHashMap();

  public TaxonomyFilterParser(MicaSearch.QueryDto queryDto) {
    this.queryDto = queryDto;
    if(queryDto != null) extractQueriedTerms();
  }

  public Map<String, Map<String, List<String>>> getTermsMap() {
    return termsMap;
  }

  private void extractQueriedTerms() {
    if(!queryDto.hasFilteredQuery()) return;
    extractQueriedTerms(queryDto.getFilteredQuery());
  }

  private void extractQueriedTerms(MicaSearch.FilteredQueryDto filteredQueryDto) {
    if(filteredQueryDto.hasExtension(MicaSearch.BoolFilterQueryDto.filter)) {
      extractQueriedTerms(filteredQueryDto.getExtension(MicaSearch.BoolFilterQueryDto.filter));
      return;
    }

    if(filteredQueryDto.hasExtension(MicaSearch.FieldFilterQueryDto.filter)) {
      extractQueriedTerms(filteredQueryDto.getExtension(MicaSearch.FieldFilterQueryDto.filter));
      return;
    }

    if(filteredQueryDto.hasExtension(MicaSearch.LogicalFilterQueryDto.filter)) {
      extractQueriedTerms(filteredQueryDto.getExtension(MicaSearch.LogicalFilterQueryDto.filter));
      return;
    }

    return;
  }

  private void extractQueriedTerms(MicaSearch.BoolFilterQueryDto boolFilterDto) {
    boolFilterDto.getFilteredQueryList().forEach(this::extractQueriedTerms);
  }

  private void extractQueriedTerms(MicaSearch.LogicalFilterQueryDto logicalFilterDto) {
    logicalFilterDto.getFieldsList().forEach(f -> extractQueriedTerms(f.getField()));
  }

  private void extractQueriedTerms(MicaSearch.FieldFilterQueryDto fieldFilterDto) {
    String name = fieldFilterDto.getField();
    if(!isAttributeField(name)) return;
    AttributeKey key = AttributeKey.from(name.replaceAll("^attributes\\.", "").replaceAll("\\.und$", ""));
    if(!key.hasNamespace()) return;

    if(!termsMap.containsKey(key.getNamespace())) {
      termsMap.put(key.getNamespace(), Maps.newHashMap());
    }
    Map<String, List<String>> vocMap = termsMap.get(key.getNamespace());
    if(!vocMap.containsKey(key.getName())) {
      vocMap.put(key.getName(), Lists.newArrayList());
    }

    if(fieldFilterDto.hasExtension(MicaSearch.TermsFilterQueryDto.terms)) {
      vocMap.get(key.getName())
        .addAll(fieldFilterDto.getExtension(MicaSearch.TermsFilterQueryDto.terms).getValuesList());
    }
  }

  private boolean isAttributeField(String name) {
    return name.startsWith("attributes.") && name.endsWith(".und");
  }

}
