/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file.search;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.obiba.mica.dataset.domain.HarmonizationDatasetState;
import org.obiba.mica.dataset.domain.StudyDatasetState;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.dataset.service.StudyDatasetService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.domain.NetworkState;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.project.domain.ProjectState;
import org.obiba.mica.project.service.ProjectService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.domain.StudyState;
import org.obiba.mica.study.service.StudyService;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

/**
 * {@link FilterBuilder} factory for files. Check the associated document ({@link org.obiba.mica.network.domain.Study},
 * {@link org.obiba.mica.network.domain.Network} etc.) is visible in order to have only (potentially) visible files in
 * the search result.
 */
@Component
public class FileFilterHelper {

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private NetworkService networkService;

  @Inject
  private StudyService studyService;

  @Inject
  private StudyDatasetService studyDatasetService;

  @Inject
  private HarmonizationDatasetService harmonizationDatasetService;

  @Inject
  private ProjectService projectService;

  /**
   * Check if the given path is part of the file search filter.
   *
   * @return
   */
  public static boolean appliesToFile(String path) {
    return path != null &&
      (path.startsWith("/network/") || path.startsWith("/study/") || path.startsWith("/study-dataset/") ||
        path.startsWith("/harmonization-dataset/") || path.startsWith("/project/"));
  }

  public QueryBuilder makeDraftFilesFilter(@NotNull String basePath) {
    List<String> networkIds = getNetworkIds(basePath, true);
    List<String> studyIds = getStudyIds(basePath, true);
    List<String> studyDatasetIds = getStudyDatasetIds(basePath, true);
    List<String> harmonizationDatasetIds = getHarmonizationDatasetIds(basePath, true);
    List<String> projectIds = getProjectIds(basePath, true);

    return makeFilterBuilder(networkIds, studyIds, studyDatasetIds, harmonizationDatasetIds, projectIds);
  }

  public QueryBuilder makePublishedFilesFilter(String basePath) {
    if(micaConfigService.getConfig().isOpenAccess()) return null;
    List<String> networkIds = getNetworkIds(basePath, false);
    List<String> studyIds = getStudyIds(basePath, false);
    List<String> studyDatasetIds = getStudyDatasetIds(basePath, false);
    List<String> harmonizationDatasetIds = getHarmonizationDatasetIds(basePath, false);
    List<String> projectIds = getProjectIds(basePath, false);

    return makeFilterBuilder(networkIds, studyIds, studyDatasetIds, harmonizationDatasetIds, projectIds);
  }

  //
  // Private methods
  //

  private List<String> getNetworkIds(String basePath, boolean draft) {
    if("/".equals(basePath) || "/network".equals(basePath)) {
      return draft
        ? networkService.findAllStates().stream().map(NetworkState::getId)
        .filter(s -> subjectAclService.isPermitted("/draft/network", "VIEW", s)).collect(Collectors.toList())
        : networkService.findPublishedStates().stream().map(NetworkState::getId)
          .filter(s -> subjectAclService.isAccessible("/network", s)).collect(Collectors.toList());
    }
    if(basePath.startsWith("/network/")) {
      String id = extractId(basePath,"/network/");
      if(draft
        ? subjectAclService.isPermitted("/draft/network", "VIEW", id)
        : subjectAclService.isAccessible("/network", id)) return Lists.newArrayList(id);
    }
    return Lists.newArrayList();
  }

  private List<String> getStudyIds(String basePath, boolean draft) {
    if("/".equals(basePath) || "/study".equals(basePath)) {
      return draft
        ? studyService.findAllStates().stream().map(StudyState::getId)
        .filter(s -> subjectAclService.isPermitted("/draft/study", "VIEW", s)).collect(Collectors.toList())
        : studyService.findPublishedStates().stream().map(StudyState::getId)
          .filter(s -> subjectAclService.isAccessible("/study", s)).collect(Collectors.toList());
    }
    if(basePath.startsWith("/study/")) {
      String id = extractId(basePath,"/study/");
      if(draft
        ? subjectAclService.isPermitted("/draft/study", "VIEW", id)
        : subjectAclService.isAccessible("/study", id)) return Lists.newArrayList(id);
    }
    return Lists.newArrayList();
  }

  private List<String> getStudyDatasetIds(String basePath, boolean draft) {
    if("/".equals(basePath) || "/study-dataset".equals(basePath)) {
      return draft
        ? studyDatasetService.findAllStates().stream().map(StudyDatasetState::getId)
        .filter(s -> subjectAclService.isPermitted("/draft/study-dataset", "VIEW", s)).collect(Collectors.toList())
        : studyDatasetService.findPublishedStates().stream().map(StudyDatasetState::getId)
          .filter(s -> subjectAclService.isAccessible("/study-dataset", s)).collect(Collectors.toList());
    }
    if(basePath.startsWith("/study-dataset/")) {
      String id = extractId(basePath,"/study-dataset/");
      if(draft
        ? subjectAclService.isPermitted("/draft/study-dataset", "VIEW", id)
        : subjectAclService.isAccessible("/study-dataset", id)) return Lists.newArrayList(id);
    }
    return Lists.newArrayList();
  }

  private List<String> getHarmonizationDatasetIds(String basePath, boolean draft) {
    if("/".equals(basePath) || "/harmonization-dataset".equals(basePath)) {
      return draft
        ? harmonizationDatasetService.findAllStates().stream().map(HarmonizationDatasetState::getId)
        .filter(s -> subjectAclService.isPermitted("/draft/harmonization-dataset", "VIEW", s))
        .collect(Collectors.toList())
        : harmonizationDatasetService.findPublishedStates().stream().map(HarmonizationDatasetState::getId)
          .filter(s -> subjectAclService.isAccessible("/harmonization-dataset", s)).collect(Collectors.toList());
    }
    if(basePath.startsWith("/harmonization-dataset/")) {
      String id = extractId(basePath,"/harmonization-dataset/");
      if(draft
        ? subjectAclService.isPermitted("/draft/harmonization-dataset", "VIEW", id)
        : subjectAclService.isAccessible("/harmonization-dataset", id)) return Lists.newArrayList(id);
    }
    return Lists.newArrayList();
  }

  private List<String> getProjectIds(String basePath, boolean draft) {
    if("/".equals(basePath) || "/project".equals(basePath)) {
      return draft
        ? projectService.findAllStates().stream().map(ProjectState::getId)
        .filter(s -> subjectAclService.isPermitted("/draft/project", "VIEW", s)).collect(Collectors.toList())
        : projectService.findPublishedStates().stream().map(ProjectState::getId)
          .filter(s -> subjectAclService.isAccessible("/project", s)).collect(Collectors.toList());
    }
    if(basePath.startsWith("/project/")) {
      String id = extractId(basePath,"/project/");
      if(draft
        ? subjectAclService.isPermitted("/draft/project", "VIEW", id)
        : subjectAclService.isAccessible("/project", id)) return Lists.newArrayList(id);
    }
    return Lists.newArrayList();
  }

  /**
   * Extract the entity identifier from base path.
   * @param basePath
   * @param replace String to be removed at the beginning of base path
   * @return
   */
  private String extractId(String basePath, String replace) {
    String p = basePath.replaceFirst(replace, "");
    int idx = p.lastIndexOf('/');
    return idx <= 0 ? p : p.substring(0, idx);
  }

  private QueryBuilder makeFilterBuilder(List<String> networkIds, List<String> studyIds, List<String> studyDatasetIds,
    List<String> harmonizationDatasetIds, List<String> projectIds) {
    List<QueryBuilder> excludes = Lists.newArrayList();
    List<QueryBuilder> includes = Lists
      .newArrayList(QueryBuilders.termQuery("path", "/user"), QueryBuilders.prefixQuery("path", "/user/"));
    addFilter(excludes, includes, "/network", networkIds);
    addFilter(excludes, includes, "/study", studyIds);
    addFilter(excludes, includes, "/study-dataset", studyDatasetIds);
    addFilter(excludes, includes, "/harmonization-dataset", harmonizationDatasetIds);
    addFilter(excludes, includes, "/project", projectIds);

    BoolQueryBuilder includedFilter = QueryBuilders.boolQuery();
    includes.forEach(includedFilter::should);
    if(excludes.isEmpty()) return includedFilter;

    BoolQueryBuilder excludedFilter = QueryBuilders.boolQuery();
    excludes.forEach(excludedFilter::should);

    return QueryBuilders.boolQuery().must(includedFilter).mustNot(excludedFilter);
  }

  private void addFilter(Collection<QueryBuilder> excludes, Collection<QueryBuilder> includes, String prefix, List<String> ids) {
    if(ids.isEmpty()) {
      excludes.add(QueryBuilders.prefixQuery("path", prefix));
    } else {
      ids.forEach(id -> {
        includes.add(QueryBuilders.termQuery("path", prefix + "/" + id));
        includes.add(QueryBuilders.prefixQuery("path", prefix + "/" + id + "/"));
      });
    }
  }

}
