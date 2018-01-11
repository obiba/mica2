/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service.helper;

import com.google.common.collect.Maps;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.service.PublishedStudyService;
import org.slf4j.Logger;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.obiba.mica.security.SubjectUtils.sudo;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class StudyIdAggregationMetaDataHelper extends AbstractStudyAggregationMetaDataHelper {

  private static final Logger log = getLogger(StudyIdAggregationMetaDataHelper.class);

  @Inject
  private PublishedStudyService publishedStudyService;

  @Cacheable(value = "aggregations-metadata", key = "'study'")
  public Map<String, AggregationMetaDataProvider.LocalizedMetaData> getStudies() {
    try {
      List<BaseStudy> studies = sudo(() -> publishedStudyService.findAll());

      return studies.stream().collect(Collectors.toMap(AbstractGitPersistable::getId, study -> {
        if (study instanceof HarmonizationStudy) {
          return new AggregationMetaDataProvider.LocalizedMetaData(study.getAcronym(), study.getName(), study.getClassName());
        }

        return new AggregationMetaDataProvider.LocalizedMetaData(study.getAcronym(), study.getName(), study.getClassName(),
            yearToString(study.getModel().get("startYear")), yearToString(study.getModel().get("endYear")));
      }));
    } catch (Exception e) {
      log.debug("Could not build Study aggregation metadata {}", e);
      return Maps.newHashMap();
    }
  }

  private String yearToString(Object year) {
    return year == null ? null : year + "";
  }

  @Override
  protected Map<String, AggregationMetaDataProvider.LocalizedMetaData> getIdAggregationMap() {
    return getStudies();
  }
}
