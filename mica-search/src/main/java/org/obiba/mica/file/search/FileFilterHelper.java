/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file.search;

import com.google.common.collect.Lists;
import org.obiba.mica.dataset.domain.HarmonizationDatasetState;
import org.obiba.mica.dataset.domain.StudyDatasetState;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.domain.NetworkState;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.project.domain.ProjectState;
import org.obiba.mica.project.service.ProjectService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.study.domain.HarmonizationStudyState;
import org.obiba.mica.study.domain.StudyState;
import org.obiba.mica.study.service.IndividualStudyService;
import org.obiba.mica.study.service.HarmonizationStudyService;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link FileFilterHelper} factory for files. Check the associated documents ({@link org.obiba.mica.study.domain.Study},
 * ({@link org.obiba.mica.study.domain.HarmonizationStudy}, {@link org.obiba.mica.network.domain.Network} etc.) is
 * visible in order to have only (potentially) visible files in
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
  private IndividualStudyService individualStudyService;

  @Inject
  private HarmonizationStudyService harmonizationStudyService;

  @Inject
  private CollectedDatasetService collectedDatasetService;

  @Inject
  private HarmonizedDatasetService harmonizedDatasetService;

  @Inject
  private ProjectService projectService;

  /**
   * Check if the given path is part of the file search filter.
   *
   * @return
   */
  public static boolean appliesToFile(String path) {
    return path != null &&
        (path.startsWith("/network/") ||
            path.startsWith("/individual-study/") ||
            path.startsWith("/harmonization-study/") ||
            path.startsWith("/collected-dataset/") ||
            path.startsWith("/harmonized-dataset/") ||
            path.startsWith("/project/"));
  }

  Searcher.PathFilter makeDraftPathFilter(@NotNull String basePath) {
    List<String> networkIds = getNetworkIds(basePath, true);
    List<String> individualStudyIds = getIndividualStudyIds(basePath, true);
    List<String> harmonizationStudyIds = getHarmonizationStudyIds(basePath, true);
    List<String> studyDatasetIds = getCollectedDatasetIds(basePath, true);
    List<String> harmonizationDatasetIds = getHarmonizedDatasetIds(basePath, true);
    List<String> projectIds = getProjectIds(basePath, true);

    return makePathFilter(networkIds, individualStudyIds, harmonizationStudyIds, studyDatasetIds,
        harmonizationDatasetIds, projectIds);
  }

  Searcher.PathFilter makePublishedPathFilter(String basePath) {
    if (micaConfigService.getConfig().isOpenAccess()) return null;
    List<String> networkIds = getNetworkIds(basePath, false);
    List<String> individualStudyIds = getIndividualStudyIds(basePath, false);
    List<String> harmonizationStudyIds = getHarmonizationStudyIds(basePath, true);
    List<String> studyDatasetIds = getCollectedDatasetIds(basePath, false);
    List<String> harmonizationDatasetIds = getHarmonizedDatasetIds(basePath, false);
    List<String> projectIds = getProjectIds(basePath, false);

    return makePathFilter(networkIds, individualStudyIds, harmonizationStudyIds, studyDatasetIds, harmonizationDatasetIds, projectIds);
  }

  //
  // Private methods
  //

  private List<String> getNetworkIds(String basePath, boolean draft) {
    if ("/".equals(basePath) || "/network".equals(basePath)) {
      return draft
          ? networkService.findAllStates().stream().map(NetworkState::getId)
          .filter(s -> subjectAclService.isPermitted("/draft/network", "VIEW", s)).collect(Collectors.toList())
          : networkService.findPublishedStates().stream().map(NetworkState::getId)
          .filter(s -> subjectAclService.isAccessible("/network", s)).collect(Collectors.toList());
    }
    if (basePath.startsWith("/network/")) {
      String id = extractId(basePath, "/network/");
      if (draft
          ? subjectAclService.isPermitted("/draft/network", "VIEW", id)
          : subjectAclService.isAccessible("/network", id)) return Lists.newArrayList(id);
    }
    return Lists.newArrayList();
  }

  private List<String> getIndividualStudyIds(String basePath, boolean draft) {
    if ("/".equals(basePath) || "/individual-study".equals(basePath)) {
      return draft
          ? individualStudyService.findAllStates().stream().map(StudyState::getId)
          .filter(s -> subjectAclService.isPermitted("/draft/individual-study", "VIEW", s)).collect(Collectors.toList())
          : individualStudyService.findPublishedStates().stream().map(StudyState::getId)
          .filter(s -> subjectAclService.isAccessible("/individual-study", s)).collect(Collectors.toList());
    }
    if (basePath.startsWith("/individual-study/")) {
      String id = extractId(basePath, "/individual-study/");
      if (draft
          ? subjectAclService.isPermitted("/draft/individual-study", "VIEW", id)
          : subjectAclService.isAccessible("/individual-study", id)) return Lists.newArrayList(id);
    }
    return Lists.newArrayList();
  }

  private List<String> getHarmonizationStudyIds(String basePath, boolean draft) {
    if ("/".equals(basePath) || "/harmonization-study".equals(basePath)) {
      return draft
          ? harmonizationStudyService.findAllStates().stream().map(HarmonizationStudyState::getId)
          .filter(s -> subjectAclService.isPermitted("/draft/harmonization-study", "VIEW", s)).collect(Collectors.toList())
          : harmonizationStudyService.findPublishedStates().stream().map(HarmonizationStudyState::getId)
          .filter(s -> subjectAclService.isAccessible("/harmonization-study", s)).collect(Collectors.toList());
    }
    if (basePath.startsWith("/harmonization-study/")) {
      String id = extractId(basePath, "/harmonization-study/");
      if (draft
          ? subjectAclService.isPermitted("/draft/harmonization-study", "VIEW", id)
          : subjectAclService.isAccessible("/harmonization-study", id)) return Lists.newArrayList(id);
    }
    return Lists.newArrayList();
  }

  private List<String> getCollectedDatasetIds(String basePath, boolean draft) {
    if ("/".equals(basePath) || "/collected-dataset".equals(basePath)) {
      return draft
          ? collectedDatasetService.findAllStates().stream().map(StudyDatasetState::getId)
          .filter(s -> subjectAclService.isPermitted("/draft/collected-dataset", "VIEW", s)).collect(Collectors.toList())
          : collectedDatasetService.findPublishedStates().stream().map(StudyDatasetState::getId)
          .filter(s -> subjectAclService.isAccessible("/collected-dataset", s)).collect(Collectors.toList());
    }
    if (basePath.startsWith("/collected-dataset/")) {
      String id = extractId(basePath, "/collected-dataset/");
      if (draft
          ? subjectAclService.isPermitted("/draft/collected-dataset", "VIEW", id)
          : subjectAclService.isAccessible("/collected-dataset", id)) return Lists.newArrayList(id);
    }
    return Lists.newArrayList();
  }

  private List<String> getHarmonizedDatasetIds(String basePath, boolean draft) {
    if ("/".equals(basePath) || "/harmonized-dataset".equals(basePath)) {
      return draft
          ? harmonizedDatasetService.findAllStates().stream().map(HarmonizationDatasetState::getId)
          .filter(s -> subjectAclService.isPermitted("/draft/harmonized-dataset", "VIEW", s))
          .collect(Collectors.toList())
          : harmonizedDatasetService.findPublishedStates().stream().map(HarmonizationDatasetState::getId)
          .filter(s -> subjectAclService.isAccessible("/harmonized-dataset", s)).collect(Collectors.toList());
    }
    if (basePath.startsWith("/harmonized-dataset/")) {
      String id = extractId(basePath, "/harmonized-dataset/");
      if (draft
          ? subjectAclService.isPermitted("/draft/harmonized-dataset", "VIEW", id)
          : subjectAclService.isAccessible("/harmonized-dataset", id)) return Lists.newArrayList(id);
    }
    return Lists.newArrayList();
  }

  private List<String> getProjectIds(String basePath, boolean draft) {
    if ("/".equals(basePath) || "/project".equals(basePath)) {
      return draft
          ? projectService.findAllStates().stream().map(ProjectState::getId)
          .filter(s -> subjectAclService.isPermitted("/draft/project", "VIEW", s)).collect(Collectors.toList())
          : projectService.findPublishedStates().stream().map(ProjectState::getId)
          .filter(s -> subjectAclService.isAccessible("/project", s)).collect(Collectors.toList());
    }
    if (basePath.startsWith("/project/")) {
      String id = extractId(basePath, "/project/");
      if (draft
          ? subjectAclService.isPermitted("/draft/project", "VIEW", id)
          : subjectAclService.isAccessible("/project", id)) return Lists.newArrayList(id);
    }
    return Lists.newArrayList();
  }

  /**
   * Extract the entity identifier from base path.
   *
   * @param basePath
   * @param replace  String to be removed at the beginning of base path
   * @return
   */
  private String extractId(String basePath, String replace) {
    String p = basePath.replaceFirst(replace, "");
    int idx = p.lastIndexOf('/');
    return idx <= 0 ? p : p.substring(0, idx);
  }

  private Searcher.PathFilter makePathFilter(List<String> networkIds, List<String> individualStudyIds,
                                             List<String> harmonizationStudyIds, List<String> collectedDatasetIds, List<String> harmonizedDatasetIds,
                                             List<String> projectIds) {

    final List<String> excludes = Lists.newArrayList();
    final List<String> includes = Lists.newArrayList("/user", "/user/");
    addPathFilter(excludes, includes, "/network", networkIds);
    addPathFilter(excludes, includes, "/individual-study", individualStudyIds);
    addPathFilter(excludes, includes, "/harmonization-study", harmonizationStudyIds);
    addPathFilter(excludes, includes, "/collected-dataset", collectedDatasetIds);
    addPathFilter(excludes, includes, "/harmonized-dataset", harmonizedDatasetIds);
    addPathFilter(excludes, includes, "/project", projectIds);

    return new Searcher.PathFilter() {
      @Override
      public Collection<String> getValues() {
        return includes;
      }

      @Override
      public Collection<String> getExcludedValues() {
        return excludes;
      }
    };
  }

  private void addPathFilter(Collection<String> excludes, Collection<String> includes, String prefix, List<String> ids) {
    if (ids.isEmpty()) {
      excludes.add(prefix);
    } else {
      ids.forEach(id -> {
        includes.add(prefix + "/" + id);
        includes.add(prefix + "/" + id + "/");
      });
    }
  }
}
