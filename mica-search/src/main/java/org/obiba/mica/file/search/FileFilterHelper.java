/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file.search;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.obiba.mica.dataset.domain.HarmonizationDatasetState;
import org.obiba.mica.dataset.domain.StudyDatasetState;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.mica.dataset.service.StudyDatasetService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.domain.NetworkState;
import org.obiba.mica.network.service.NetworkService;
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

  /**
   * Check if the given path is part of the file search filter.
   *
   * @return
   */
  public static boolean appliesToFile(String path) {
    return path != null &&
      (path.startsWith("/network/") || path.startsWith("/study/") || path.startsWith("/study-dataset/") ||
        path.startsWith("/harmonization-dataset/"));
  }

  public FilterBuilder makeDraftFilesFilter(@NotNull String basePath) {
    List<String> networkIds = getNetworkIds(basePath, true);
    List<String> studyIds = getStudyIds(basePath, true);
    List<String> studyDatasetIds = getStudyDatasetIds(basePath, true);
    List<String> harmonizationDatasetIds = getHarmonizationDatasetIds(basePath, true);

    return makeFilterBuilder(networkIds, studyIds, studyDatasetIds, harmonizationDatasetIds);
  }

  public FilterBuilder makePublishedFilesFilter(String basePath) {
    if(micaConfigService.getConfig().isOpenAccess()) return null;
    List<String> networkIds = getNetworkIds(basePath, false);
    List<String> studyIds = getStudyIds(basePath, false);
    List<String> studyDatasetIds = getStudyDatasetIds(basePath, false);
    List<String> harmonizationDatasetIds = getHarmonizationDatasetIds(basePath, false);

    return makeFilterBuilder(networkIds, studyIds, studyDatasetIds, harmonizationDatasetIds);
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

  private FilterBuilder makeFilterBuilder(List<String> networkIds, List<String> studyIds, List<String> studyDatasetIds,
    List<String> harmonizationDatasetIds) {
    List<FilterBuilder> excludes = Lists.newArrayList();
    List<FilterBuilder> includes = Lists
      .newArrayList(FilterBuilders.termFilter("path", "/user"), FilterBuilders.prefixFilter("path", "/user/"));
    addFilter(excludes, includes, "/network", networkIds);
    addFilter(excludes, includes, "/study", studyIds);
    addFilter(excludes, includes, "/study-dataset", studyDatasetIds);
    addFilter(excludes, includes, "/harmonization-dataset", harmonizationDatasetIds);

    FilterBuilder includedFilter = FilterBuilders.orFilter(includes.toArray(new FilterBuilder[includes.size()]));
    if(excludes.isEmpty()) return includedFilter;

    FilterBuilder excludedFilter = FilterBuilders
      .notFilter(FilterBuilders.orFilter(excludes.toArray(new FilterBuilder[excludes.size()])));

    return FilterBuilders.andFilter(excludedFilter, includedFilter);
  }

  private void addFilter(List<FilterBuilder> excludes, List<FilterBuilder> includes, String prefix, List<String> ids) {
    if(ids.isEmpty()) {
      excludes.add(FilterBuilders.prefixFilter("path", prefix));
    } else {
      ids.forEach(id -> {
        includes.add(FilterBuilders.termFilter("path", prefix + "/" + id));
        includes.add(FilterBuilders.prefixFilter("path", prefix + "/" + id + "/"));
      });
    }
  }

}
