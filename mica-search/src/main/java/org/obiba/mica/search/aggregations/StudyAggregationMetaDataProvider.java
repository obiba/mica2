/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.aggregations;

import java.util.Map;

import javax.inject.Inject;

import org.obiba.mica.micaConfig.service.helper.AggregationMetaDataProvider;
import org.obiba.mica.micaConfig.service.helper.StudyIdAggregationMetaDataHelper;
import org.springframework.stereotype.Component;

@Component
public class StudyAggregationMetaDataProvider implements AggregationMetaDataProvider {

  private static final String AGGREGATION_NAME = "studyIds";

  @Inject
  StudyIdAggregationMetaDataHelper helper;

  @Override
  public MetaData getMetadata(String aggregation, String termKey, String locale) {
    Map<String, LocalizedMetaData> studiesDictionary = helper.getStudies();
    return AGGREGATION_NAME.equals(aggregation) && studiesDictionary.containsKey(termKey) ? MetaData.newBuilder() //
      .title(studiesDictionary.get(termKey).getTitle().get(locale)) //
      .description(studiesDictionary.get(termKey).getDescription().get(locale)) //
      .className(studiesDictionary.get(termKey).getClassName())
      .start(studiesDictionary.get(termKey).getStart()) //
      .end(studiesDictionary.get(termKey).getEnd()) //
      .sortField(studiesDictionary.get(termKey).getSortField())
      .build() : null;
  }

  @Override
  public boolean containsAggregation(String aggregation) {
    return AGGREGATION_NAME.equals(aggregation);
  }

  @Override
  public void refresh() {
  }

}
