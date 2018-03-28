/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest.entity;

import org.obiba.mica.dataset.rest.entity.rql.RQLCriteriaOpalConverter;
import org.obiba.mica.dataset.rest.entity.rql.RQLCriterionOpalConverter;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.web.model.DocumentDigestDtos;
import org.obiba.mica.web.model.LocalizedStringDtos;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class StudyEntitiesCountService {

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private StudyService studyService;

  @Inject
  private OpalService opalService;

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private DocumentDigestDtos documentDigestDtos;

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  /**
   * Parse the RQL query and translate each node as a Opal query wrapped in a {@link StudyEntitiesCountQuery}.
   *
   * @param query
   * @param entityType
   * @return
   */
  public List<StudyEntitiesCountQuery> newQueries(String query, String entityType) {
    RQLCriteriaOpalConverter converter = applicationContext.getBean(RQLCriteriaOpalConverter.class);
    converter.parse(query);
    Map<BaseStudy, List<RQLCriterionOpalConverter>> studyConverters = converter.getCriterionConverters().stream()
      .filter(c -> !c.hasMultipleStudyTables()) // TODO include Dataschema variables
      .collect(Collectors.groupingBy(c -> c.getVariableReferences().getStudy()));

    return studyConverters.keySet().stream()
      .map(study -> newQuery(entityType, study, studyConverters.get(study)))
      .collect(Collectors.toList());
  }

  private StudyEntitiesCountQuery newQuery(String entityType, BaseStudy study, List<RQLCriterionOpalConverter> rqlCriterionOpalConverters) {
    return new StudyEntitiesCountQuery(studyService, opalService, micaConfigService.getConfig().getPrivacyThreshold(),
      documentDigestDtos, localizedStringDtos, entityType, study, rqlCriterionOpalConverters);
  }
}
