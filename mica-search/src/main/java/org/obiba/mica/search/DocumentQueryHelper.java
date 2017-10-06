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

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.obiba.mica.search.queries.DocumentQueryJoinKeys;

public final class DocumentQueryHelper {

  public static QueryBuilder updateDatasetJoinKeysQuery(QueryBuilder queryBuilder, String datasetIdKey, DocumentQueryIdProvider idProvider) {
    if(idProvider != null) {
      List<String> datasetIds = idProvider.getIds();
      if(datasetIds.size() > 0) {
        TermsQueryBuilder termsQueryBuilder = QueryBuilders.termsQuery(datasetIdKey, datasetIds);
        return QueryBuilders.boolQuery().must(queryBuilder).must(termsQueryBuilder);
      }
    }

    return queryBuilder;
  }

  public static DocumentQueryJoinKeys processStudyJoinKey(SearchResponse response) {
    DocumentQueryJoinKeys joinKeys = new DocumentQueryJoinKeys();
    response.getAggregations().forEach(aggregation -> ((Terms) aggregation).getBuckets().forEach(bucket -> {
      if(bucket.getDocCount() > 0) {
        joinKeys.studyIds.add(bucket.getKeyAsString());
      }
    }));

    return joinKeys;
  }

  public static DocumentQueryJoinKeys processDatasetJoinKeys(SearchResponse response, String datasetId, DocumentQueryIdProvider idProvider) {
    DocumentQueryJoinKeys joinKeys = new DocumentQueryJoinKeys();
    response.getAggregations().forEach(
      aggregation -> ((Terms) aggregation).getBuckets().forEach(
        bucket -> {
          List<String> list = datasetId.equals(aggregation.getName()) ? joinKeys.datasetIds : joinKeys.studyIds;
          if (bucket.getDocCount() > 0)
            list.add(bucket.getKeyAsString());
        }));

    if(idProvider != null) idProvider.setIds(joinKeys.datasetIds);
    return joinKeys;
  }
}
