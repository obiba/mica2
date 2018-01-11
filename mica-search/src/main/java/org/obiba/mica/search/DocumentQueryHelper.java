/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search;

import com.google.common.base.Joiner;
import org.obiba.mica.search.queries.DocumentQueryJoinKeys;
import org.obiba.mica.spi.search.Searcher;

import java.util.List;

public final class DocumentQueryHelper {

  public static String inRQL(String field, DocumentQueryIdProvider idProvider) {
    if (idProvider == null) return "";
    List<String> ids = idProvider.getIds();
    if (ids == null || ids.isEmpty()) return "";
    return String.format("in(%s,(%s))", field, Joiner.on(",").join(ids));
  }

  public static DocumentQueryJoinKeys processStudyJoinKey(Searcher.DocumentResults results) {
    DocumentQueryJoinKeys joinKeys = new DocumentQueryJoinKeys();
    results.getAggregations().stream().filter(agg -> agg.getType().equals("terms"))
        .forEach(aggregation -> aggregation.asTerms().getBuckets().forEach(bucket -> {
          if (bucket.getDocCount() > 0) {
            joinKeys.studyIds.add(bucket.getKeyAsString());
          }
        }));

    return joinKeys;
  }

  public static DocumentQueryJoinKeys processDatasetJoinKeys(Searcher.DocumentResults results, String datasetId, DocumentQueryIdProvider idProvider) {
    DocumentQueryJoinKeys joinKeys = new DocumentQueryJoinKeys();
    results.getAggregations().stream().filter(agg -> agg.getType().equals("terms"))
        .forEach(
            aggregation -> aggregation.asTerms().getBuckets().forEach(
                bucket -> {
                  List<String> list = datasetId.equals(aggregation.getName()) ? joinKeys.datasetIds : joinKeys.studyIds;
                  if (bucket.getDocCount() > 0)
                    list.add(bucket.getKeyAsString());
                }));

    if (idProvider != null) idProvider.setIds(joinKeys.datasetIds);
    return joinKeys;
  }
}
