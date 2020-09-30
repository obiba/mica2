/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.ForbiddenException;

import org.apache.shiro.SecurityUtils;
import org.obiba.magma.NoSuchVariableException;
import org.obiba.mica.core.domain.BaseStudyTable;
import org.obiba.mica.core.domain.OpalTable;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

/**
 * Retrieve the {@link org.obiba.mica.dataset.domain.Dataset} from the published dataset index.
 *
 * @param <T>
 */
public abstract class AbstractPublishedDatasetResource<T extends Dataset> {

  private static final Logger log = LoggerFactory.getLogger(AbstractPublishedDatasetResource.class);

  @Inject
  protected ApplicationContext applicationContext;

  @Inject
  protected Dtos dtos;

  @Inject
  protected Searcher searcher;

  @Inject
  protected ObjectMapper objectMapper;

  @Inject
  protected OpalService opalService;

  @Inject
  protected MicaConfigService micaConfigService;

  @Inject
  protected SubjectAclService subjectAclService;

  private String locale;

  protected T getDataset(Class<T> clazz, @NotNull String datasetId) throws NoSuchDatasetException {
    InputStream inputStream = searcher.getDocumentByClassName(Indexer.PUBLISHED_DATASET_INDEX, Indexer.DATASET_TYPE, clazz, datasetId);
    if (inputStream == null) throw NoSuchDatasetException.withId(datasetId);
    try {
      T rval = objectMapper.readValue(inputStream, clazz);
      log.debug("Response /{}/{}", Indexer.PUBLISHED_DATASET_INDEX, Indexer.DATASET_TYPE);
      return rval;
    } catch (IOException e) {
      log.error("Failed retrieving {}", clazz.getSimpleName(), e);
      throw NoSuchDatasetException.withId(datasetId);
    }
  }

  protected Mica.DatasetDto getDatasetDto(Class<T> clazz, @NotNull String datasetId) throws NoSuchDatasetException {
    return dtos.asDto(getDataset(clazz, datasetId));
  }

  protected Mica.DatasetVariablesDto getDatasetVariableDtos(@NotNull String queryString, @NotNull String datasetId,
                                                            DatasetVariable.Type type, int from, int limit, @Nullable String sort, @Nullable String order) {
    // TODO make a helper class for this
    String queryStr = "";
    if (!Strings.isNullOrEmpty(queryString)) {
      queryStr = String.format(",query(%s)", queryString.replaceAll(" ","+"));
    }
    String rql = String.format("and(eq(datasetId,%s),eq(variableType,%s)%s)", datasetId, type.toString(), queryStr);
    return getDatasetVariableDtosInternal(rql, from, limit, sort, order);
  }

  protected Mica.DatasetVariablesDto getDatasetVariableDtos(@NotNull String datasetId, DatasetVariable.Type type, int from,
                                                            int limit, @Nullable String sort, @Nullable String order) {
    String rql = String.format("and(eq(datasetId,%s),eq(variableType,%s))", datasetId, type.toString());
    return getDatasetVariableDtosInternal(rql, from, limit, sort, order);
  }

  protected Mica.DatasetVariablesDto getDatasetVariableDtosInternal(String rql, int from, int limit, @Nullable String sort, @Nullable String order) {
    String rqlSort = "";
    if (!Strings.isNullOrEmpty(sort)) {
      String orderOp = !Strings.isNullOrEmpty(order) && "DESC".equals(order.toUpperCase()) ? "-" : "";
      rqlSort = String.format(",sort(%s)", orderOp + sort);
    }
    String query = String.format("variable(%s,limit(%s,%s)%s)", rql, from, limit, rqlSort);
    Searcher.DocumentResults results = searcher.find(Indexer.PUBLISHED_VARIABLE_INDEX, Indexer.VARIABLE_TYPE, query);

    Mica.DatasetVariablesDto.Builder builder = Mica.DatasetVariablesDto.newBuilder()
        .setTotal(Long.valueOf(results.getTotal()).intValue())
        .setFrom(from)
        .setLimit(limit);

    List<Taxonomy> taxonomies = getTaxonomies();

    Map<String, List<DatasetVariable>> studyIdVariableMap = results.getDocuments().stream().map(res -> {
      try {
        return objectMapper.readValue(res.getSourceInputStream(), DatasetVariable.class);
      } catch(IOException e) {
        log.error("Failed retrieving {}", DatasetVariable.class.getSimpleName(), e);
        return null;
      }
    }).filter(Objects::nonNull).collect(Collectors.groupingBy(DatasetVariable::getStudyId));

    builder.addAllVariables(dtos.asDtoList(studyIdVariableMap, taxonomies));

    log.info("Response /{}/{}", Indexer.PUBLISHED_VARIABLE_INDEX, Indexer.VARIABLE_TYPE);

    return builder.build();
  }

  protected List<DatasetVariable> getDatasetVariablesInternal(String rql, int from, int limit, @Nullable String sort, @Nullable String order, boolean harmonized) {
    String rqlSort = "";
    if (!Strings.isNullOrEmpty(sort)) {
      String orderOp = !Strings.isNullOrEmpty(order) && "DESC".equals(order.toUpperCase()) ? "-" : "";
      rqlSort = String.format(",sort(%s)", orderOp + sort);
    }
    String query = String.format("variable(%s,limit(%s,%s)%s)", rql, from, limit, rqlSort);
    Searcher.DocumentResults results = searcher.find(harmonized ? Indexer.PUBLISHED_HVARIABLE_INDEX : Indexer.PUBLISHED_VARIABLE_INDEX,
      harmonized ? Indexer.HARMONIZED_VARIABLE_TYPE : Indexer.VARIABLE_TYPE, query);

    List<DatasetVariable> variables = results.getDocuments().stream().map(res -> {
      try {
        return objectMapper.readValue(res.getSourceInputStream(), DatasetVariable.class);
      } catch(IOException e) {
        log.error("Failed retrieving {}", DatasetVariable.class.getSimpleName(), e);
        return null;
      }
    }).filter(Objects::nonNull).collect(Collectors.toList());

    log.info("Response /{}/{}", Indexer.PUBLISHED_VARIABLE_INDEX, Indexer.VARIABLE_TYPE);

    return variables;
  }

  protected Mica.DatasetVariableHarmonizationDto getVariableHarmonizationDto(HarmonizationDataset dataset,
                                                                             String variableName, boolean includeSummaries) {
    DatasetVariable.IdResolver variableResolver = DatasetVariable.IdResolver
        .from(dataset.getId(), variableName, DatasetVariable.Type.Dataschema);
    Mica.DatasetVariableHarmonizationDto.Builder builder = Mica.DatasetVariableHarmonizationDto.newBuilder();
    builder.setResolver(dtos.asDto(variableResolver));

    dataset.getBaseStudyTables().forEach(table -> {
      try {
        builder.addDatasetVariableSummaries(
            getDatasetVariableSummaryDto(dataset.getId(), variableResolver.getName(), DatasetVariable.Type.Harmonized,
                table, includeSummaries));
      } catch (NoSuchVariableException e) {
        log.debug("ignore (case the study has not implemented this dataschema variable)", e);
      }
    });

    return builder.build();
  }

  protected Mica.DatasetVariableHarmonizationSummaryDto getVariableHarmonizationSummaryDto(HarmonizationDataset dataset, String variableName) {
    DatasetVariable.IdResolver variableResolver = DatasetVariable.IdResolver
      .from(dataset.getId(), variableName, DatasetVariable.Type.Dataschema);
    Mica.DatasetVariableHarmonizationSummaryDto.Builder builder = Mica.DatasetVariableHarmonizationSummaryDto.newBuilder();
    builder.setDataschemaVariableRef(dtos.asDto(variableResolver));

    dataset.getBaseStudyTables().forEach(table ->
      builder.addHarmonizedVariables(getDatasetHarmonizedVariableSummaryDto(dataset.getId(), variableResolver.getName(), DatasetVariable.Type.Harmonized, table)));

    return builder.build();
  }

  protected DatasetVariable getDatasetVariable(@NotNull String datasetId, @NotNull String variableName,
                                               DatasetVariable.Type variableType, OpalTable opalTable) {

    if (opalTable != null) {
      return getDatasetVariable(datasetId,
          variableName,
          variableType,
          opalTable instanceof BaseStudyTable ? ((BaseStudyTable) opalTable).getStudyId() : null,
          opalTable.getProject(),
          opalTable.getTable(),
          opalTable instanceof StudyTable
              ? DatasetVariable.OPAL_STUDY_TABLE_PREFIX
              : DatasetVariable.OPAL_HARMONIZATION_TABLE_PREFIX);

    }

    return getDatasetVariable(datasetId, variableName, variableType, null, null, null, null);
  }

  /**
   * Look for a variable of a dataset in the published dataset index or the published study index depending on
   * whether a study ID is provided for resolving variable's parent.
   *
   * @param datasetId
   * @param variableName
   * @param studyId
   * @param project
   * @param table
   * @return
   * @throws NoSuchVariableException
   */
  protected DatasetVariable getDatasetVariable(@NotNull String datasetId, @NotNull String variableName,
                                               DatasetVariable.Type variableType, @Nullable String studyId, @Nullable String project, @Nullable String table,
                                               @Nullable String tableType)
      throws NoSuchVariableException {

    String variableId = DatasetVariable.IdResolver
        .encode(datasetId, variableName, variableType, studyId, project, table, tableType);

    if (variableType.equals(DatasetVariable.Type.Harmonized)) {
      return getHarmonizedDatasetVariable(datasetId, variableId, variableName);
    }

    return getDatasetVariableInternal(Indexer.PUBLISHED_VARIABLE_INDEX, Indexer.VARIABLE_TYPE, variableId, variableName);
  }

  protected Mica.DatasetVariableDto getDatasetVariableDto(@NotNull String datasetId, @NotNull String variableName,
                                                          DatasetVariable.Type variableType) {
    return getDatasetVariableDto(datasetId, variableName, variableType, null);
  }

  protected Mica.DatasetVariableDto getDatasetVariableDto(@NotNull String datasetId, @NotNull String variableName,
                                                          DatasetVariable.Type variableType, @Nullable OpalTable opalTable) {
    return dtos
        .asDto(getDatasetVariable(datasetId, variableName, variableType, opalTable), getTaxonomies(),
            getLocale());
  }

  protected Mica.DatasetVariableDto getDatasetVariableDto(@NotNull String datasetId, @NotNull String variableName,
                                                          DatasetVariable.Type variableType, @Nullable String studyId, @Nullable String project, @Nullable String table,
                                                          @Nullable String tableType) {
    return dtos.asDto(getDatasetVariable(datasetId, variableName, variableType, studyId, project, table, tableType),
        getTaxonomies(), getLocale());
  }

  protected Mica.DatasetHarmonizedVariableSummaryDto getDatasetHarmonizedVariableSummaryDto(@NotNull String datasetId,
                                                                        @NotNull String variableName, DatasetVariable.Type variableType, @Nullable OpalTable opalTable) {
    try {
      DatasetVariable variable = getDatasetVariable(datasetId, variableName, variableType, opalTable);
      return dtos.asHarmonizedSummaryDto(variable);
    } catch (NoSuchVariableException e) {
      return Mica.DatasetHarmonizedVariableSummaryDto.newBuilder().setStatus("").build();
    }
  }


  protected Mica.DatasetVariableSummaryDto getDatasetVariableSummaryDto(@NotNull String datasetId,
                                                                        @NotNull String variableName, DatasetVariable.Type variableType, @Nullable OpalTable opalTable) {
    DatasetVariable variable = getDatasetVariable(datasetId, variableName, variableType, opalTable);
    return dtos.asSummaryDto(variable, opalTable, true);
  }

  protected Mica.DatasetVariableSummaryDto getDatasetVariableSummaryDto(@NotNull String datasetId,
                                                                        @NotNull String variableName,
                                                                        DatasetVariable.Type variableType,
                                                                        @Nullable OpalTable opalTable,
                                                                        boolean includeSummaries) {
    DatasetVariable variable = getDatasetVariable(datasetId, variableName, variableType, opalTable);
    return dtos.asSummaryDto(variable, opalTable, includeSummaries);
  }

  @NotNull
  protected List<Taxonomy> getTaxonomies() {
    List<Taxonomy> taxonomies = null;
    try {
      taxonomies = opalService.getTaxonomies();
    } catch (Exception e) {
      // ignore
    }
    return taxonomies == null ? Collections.emptyList() : taxonomies;
  }

  protected void checkContingencyAccess() {
    if (!micaConfigService.getConfig().isContingencyEnabled())
      throw new ForbiddenException();
  }

  protected void checkVariableSummaryAccess() {
    if (!SecurityUtils.getSubject().isAuthenticated() && micaConfigService.getConfig().isVariableSummaryRequiresAuthentication())
      throw new ForbiddenException();
  }

  private DatasetVariable getHarmonizedDatasetVariable(String datasetId, String variableId, String variableName) {
    String dataSchemaVariableId = DatasetVariable.IdResolver
        .encode(datasetId, variableName, DatasetVariable.Type.Dataschema, null, null, null, null);
    DatasetVariable harmonizedDatasetVariable = getDatasetVariableInternal(Indexer.PUBLISHED_HVARIABLE_INDEX, Indexer.HARMONIZED_VARIABLE_TYPE,
        variableId, variableName);
    DatasetVariable dataSchemaVariable = getDatasetVariableInternal(Indexer.PUBLISHED_VARIABLE_INDEX, Indexer.VARIABLE_TYPE,
        dataSchemaVariableId, variableName);

    dataSchemaVariable.getAttributes().asAttributeList().forEach(a -> {
      if (!a.getName().startsWith("Mlstr_harmo")) harmonizedDatasetVariable.addAttribute(a);
    });

    return harmonizedDatasetVariable;
  }

  private DatasetVariable getDatasetVariableInternal(String indexName, String indexType, String variableId, String variableName) {
    InputStream inputStream = searcher.getDocumentById(indexName, indexType, variableId);
    if (inputStream == null) throw new NoSuchVariableException(variableName);
    try {
      return objectMapper.readValue(inputStream, DatasetVariable.class);
    } catch (IOException e) {
      log.error("Failed retrieving {}", DatasetVariable.class.getSimpleName(), e);
      throw new NoSuchVariableException(variableName);
    }
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String value) {
    locale = value;
  }
}
