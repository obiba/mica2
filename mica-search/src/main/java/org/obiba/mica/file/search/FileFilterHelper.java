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
      (path.startsWith("/network") || path.startsWith("/study") || path.startsWith("/study-dataset") ||
        path.startsWith("/harmonization-dataset"));
  }

  public FilterBuilder makeDraftFilesFilter() {
    List<String> networkIds = networkService.findPublishedStates().stream().map(NetworkState::getId)
      .filter(s -> subjectAclService.isPermitted("/draft/network", "VIEW", s)).collect(Collectors.toList());
    List<String> studyIds = studyService.findPublishedStates().stream().map(StudyState::getId)
      .filter(s -> subjectAclService.isPermitted("/draft/study", "VIEW", s)).collect(Collectors.toList());
    List<String> studyDatasetIds = studyDatasetService.findPublishedStates().stream().map(StudyDatasetState::getId)
      .filter(s -> subjectAclService.isPermitted("/draft/study-dataset", "VIEW", s)).collect(Collectors.toList());
    List<String> harmonizationDatasetIds = harmonizationDatasetService.findPublishedStates().stream()
      .map(HarmonizationDatasetState::getId)
      .filter(s -> subjectAclService.isPermitted("/draft/harmonization-dataset", "VIEW", s))
      .collect(Collectors.toList());

    return makeFilterBuilder(networkIds, studyIds, studyDatasetIds, harmonizationDatasetIds);
  }

  public FilterBuilder makePublishedFilesFilter() {
    if(micaConfigService.getConfig().isOpenAccess()) return null;
    List<String> networkIds = networkService.findPublishedStates().stream().map(NetworkState::getId)
      .filter(s -> subjectAclService.isAccessible("/network", s)).collect(Collectors.toList());
    List<String> studyIds = studyService.findPublishedStates().stream().map(StudyState::getId)
      .filter(s -> subjectAclService.isAccessible("/study", s)).collect(Collectors.toList());
    List<String> studyDatasetIds = studyDatasetService.findPublishedStates().stream().map(StudyDatasetState::getId)
      .filter(s -> subjectAclService.isAccessible("/study-dataset", s)).collect(Collectors.toList());
    List<String> harmonizationDatasetIds = harmonizationDatasetService.findPublishedStates().stream()
      .map(HarmonizationDatasetState::getId).filter(s -> subjectAclService.isAccessible("/harmonization-dataset", s))
      .collect(Collectors.toList());

    return makeFilterBuilder(networkIds, studyIds, studyDatasetIds, harmonizationDatasetIds);
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
