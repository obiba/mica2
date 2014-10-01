/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.queries;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.obiba.mica.core.domain.AttributeKey;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.search.VariableIndexer;
import org.obiba.mica.micaConfig.OpalService;
import org.obiba.mica.study.NoSuchStudyException;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

import sun.util.locale.LanguageTag;

@Component
public class VariableQuery extends AbstractDocumentQuery {

  private static final String VARIABLE_FACETS_YML = "variable-facets.yml";

  private static final String JOIN_FIELD = "studyIds";

  @Inject
  private OpalService opalService;

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private Dtos dtos;

  @Inject
  private ObjectMapper objectMapper;

  @Override
  public String getSearchIndex() {
    return VariableIndexer.PUBLISHED_VARIABLE_INDEX;
  }

  @Override
  public String getSearchType() {
    return VariableIndexer.VARIABLE_TYPE;
  }

  @Override
  protected void processHits(MicaSearch.QueryResultDto.Builder builder, SearchHits hits) throws IOException {
    MicaSearch.DatasetVariableResultDto.Builder resBuilder = MicaSearch.DatasetVariableResultDto.newBuilder();
    Map<String, Study> studyMap = Maps.newHashMap();

    for(SearchHit hit : hits) {
      DatasetVariable.IdResolver resolver = DatasetVariable.IdResolver.from(hit.getId());
      InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes());
      DatasetVariable variable = objectMapper.readValue(inputStream, DatasetVariable.class);
      resBuilder.addSummaries(processHit(resolver, variable, studyMap));
    }

    builder.setExtension(MicaSearch.DatasetVariableResultDto.result, resBuilder.build());
  }

  @Override
  protected boolean ignoreFields() {
    // always needs actual documents
    return false;
  }

  /**
   * Fill the variable resolver dto with user-friendly labels about the variable, the dataset and the study.
   *
   * @param resolver
   * @param variable
   * @param studyMap study cache
   * @return
   */
  private Mica.DatasetVariableResolverDto processHit(DatasetVariable.IdResolver resolver, DatasetVariable variable,
      Map<String, Study> studyMap) {
    Mica.DatasetVariableResolverDto.Builder builder = dtos.asDto(resolver);

    String studyId = resolver.hasStudyId() ? resolver.getStudyId() : null;

    if(resolver.getType().equals(DatasetVariable.Type.Study) ||
        resolver.getType().equals(DatasetVariable.Type.Harmonized)) {
      studyId = variable.getStudyIds().get(0);
    }

    if(studyId != null) {
      builder.setStudyId(studyId);
      try {
        Study study;
        if(studyMap.containsKey(studyId)) {
          study = studyMap.get(studyId);
        } else {
          study = publishedStudyService.findById(studyId);
          studyMap.put(studyId, study);
        }

        builder.addAllStudyName(dtos.asDto(study.getName()));
        builder.addAllStudyAcronym(dtos.asDto(study.getAcronym()));
      } catch(NoSuchStudyException e) {
      }
    }

    builder.addAllDatasetName(dtos.asDto(variable.getDatasetName()));

    if(variable.hasAttribute("label", null)) {
      builder.addAllVariableLabel(dtos.asDto(variable.getAttributes().getAttribute("label", null).getValues()));
    }

    return builder.build();
  }

  @Override
  public void queryAggrations(List<String> studyIds) throws IOException {
    queryAggregations(studyIds, false);
  }

  @Override
  protected Resource getAggregationsDescription() {
    return new ClassPathResource(VARIABLE_FACETS_YML);
  }

  @Nullable
  @Override
  public Map<String, Integer> getStudyCounts() {
    return getStudyCounts(JOIN_FIELD);
  }

  @Override
  protected List<String> getJoinFields() {
    return Arrays.asList(JOIN_FIELD);
  }

  protected Properties getAggregationsProperties() {
    Properties properties = new Properties();
    getTaxonomies().forEach(taxonomy -> {
      if(taxonomy.hasVocabularies()) {
        taxonomy.getVocabularies().forEach(vocabulary -> {
          if(vocabulary.hasTerms()) {
            properties.put("attributes." + AttributeKey.getMapKey(vocabulary.getName(), taxonomy.getName()) + "." +
                LanguageTag.UNDETERMINED, "");
          }
        });
      }
    });
    return properties;
  }

  @NotNull
  protected List<Taxonomy> getTaxonomies() {
    List<Taxonomy> taxonomies = null;
    try {
      taxonomies = opalService.getTaxonomies();
    } catch(Exception e) {
      // ignore
    }
    return taxonomies == null ? Collections.emptyList() : taxonomies;
  }
}
