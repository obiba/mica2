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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.search.queries.DatasetQuery;
import org.obiba.mica.spi.search.CountStatsData;
import org.obiba.mica.study.domain.BaseStudy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static org.obiba.mica.web.model.Mica.CountStatsDto;


public class CountStatsDtoBuilders {

  private CountStatsDtoBuilders() {}

  public static class AbstractCountStatsBuilder {

    protected final CountStatsData countStatsData;

    private AbstractCountStatsBuilder(CountStatsData data) {
      countStatsData = data;
    }
  }

  public static class DatasetCountStatsBuilder extends AbstractCountStatsBuilder {

    private DatasetCountStatsBuilder(CountStatsData data) {
      super(data);
    }

    public static DatasetCountStatsBuilder newBuilder(CountStatsData countStatsData) {
      return new DatasetCountStatsBuilder(countStatsData);
    }

    public CountStatsDto build(Dataset dataset) {
      return calculateCounts(dataset, getStudyIds(dataset));
    }

    private CountStatsDto calculateCounts(Dataset dataset, List<String> ids) {
      int individualStudies = 0;
      int harmonizationStudies = 0;

      List<String> networks = Lists.newArrayList();
      for(String id : ids) {
        individualStudies += countStatsData.getIndividualStudies(id);
        harmonizationStudies += countStatsData.getHarmonizationStudies(id);
        networks.addAll(countStatsData.getNetworks(id));
      }

      CountStatsDto.Builder builder = CountStatsDto.newBuilder()
        .setVariables(countStatsData.getVariables(dataset.getId()))
        .setStudies(individualStudies + harmonizationStudies)
        .setIndividualStudies(individualStudies)
        .setHarmonizationStudies(harmonizationStudies);
      int networksCount = (int) networks.stream().distinct().count();
      builder.setNetworks(networksCount);

      return builder.build();
    }

    private List<String> getStudyIds(Dataset dataset) {
      List<String> ids = new ArrayList<>();
      if(dataset instanceof StudyDataset) {
        StudyDataset sDataset = (StudyDataset) dataset;
        if(sDataset.hasStudyTable()) {
          ids.add(sDataset.getStudyTable().getStudyId());
        }
      } else {
        HarmonizationDataset hDataset = (HarmonizationDataset) dataset;
        if (hDataset.hasHarmonizationTable()) {
          ids.add(hDataset.getHarmonizationTable().getStudyId());
        }
      }

      return ids;
    }

  }

  public static class NetworkCountStatsBuilder extends AbstractCountStatsBuilder {

    private NetworkCountStatsBuilder(CountStatsData data) {
      super(data);
    }

    public static NetworkCountStatsBuilder newBuilder(CountStatsData countStatsData) {
      return new NetworkCountStatsBuilder(countStatsData);
    }

    public CountStatsDto build(Network network) {

      List<String> studyIdsInRequestResponse = network.getStudyIds().stream()
        .filter(studyIdInNetwork -> this.countStatsData.getStudies(studyIdInNetwork) > 0)
        .collect(toList());

      return calculateCounts(studyIdsInRequestResponse);
    }

    private CountStatsDto calculateCounts(List<String> ids) {
      List<String> studyDatasets = Lists.newArrayList();
      List<String> harmonizationDatasets = Lists.newArrayList();

      int individualStudies = 0;
      int harmonizationStudies = 0;

      for(String id : ids) {
        Map<String, List<String>> datasets = countStatsData.getDataset(id);

        if(datasets.containsKey(DatasetQuery.STUDY_JOIN_FIELD)) {
          studyDatasets.addAll(datasets.get(DatasetQuery.STUDY_JOIN_FIELD));
        }

        if(datasets.containsKey(DatasetQuery.HARMONIZATION_STUDY_JOIN_FIELD)) {
          harmonizationDatasets.addAll(datasets.get(DatasetQuery.HARMONIZATION_STUDY_JOIN_FIELD));
        }

        individualStudies += countStatsData.getIndividualStudies(id);
        harmonizationStudies += countStatsData.getHarmonizationStudies(id);
      }

      studyDatasets = studyDatasets.stream().distinct().collect(toList());
      harmonizationDatasets = harmonizationDatasets.stream().distinct().collect(toList());
      int variables = Sets.union(ImmutableSet.copyOf(studyDatasets), ImmutableSet.copyOf(harmonizationDatasets))
        .stream().mapToInt(countStatsData::getVariables).sum();
      int studyVariables = studyDatasets.stream().mapToInt(countStatsData::getVariables).sum();
      int dataschemaVariables = harmonizationDatasets.stream().mapToInt(countStatsData::getVariables).sum();

      return CountStatsDto.newBuilder().setVariables(variables)
          .setStudyVariables(studyVariables)
          .setDataschemaVariables(dataschemaVariables)
          .setStudyDatasets(studyDatasets.size())
          .setHarmonizationDatasets(harmonizationDatasets.size())
          .setStudies(individualStudies + harmonizationStudies)
          .setIndividualStudies(individualStudies)
          .setHarmonizationStudies(harmonizationStudies)
          .setStudiesWithVariables((int) ids.stream()
              .filter(i -> countStatsData.getDataset(i).containsKey(DatasetQuery.STUDY_JOIN_FIELD)).count()) // only collected study variables
          .setHarmonizationStudiesWithVariables((int) ids.stream()
            .filter(i -> countStatsData.getDataset(i).containsKey(DatasetQuery.HARMONIZATION_STUDY_JOIN_FIELD)).count()) // only harmonized study variables
          .build();
    }
  }

  public static class StudyCountStatsBuilder extends AbstractCountStatsBuilder {

    private StudyCountStatsBuilder(CountStatsData data) {
      super(data);
    }

    public static StudyCountStatsBuilder newBuilder(CountStatsData countStatsData) {
      return new StudyCountStatsBuilder(countStatsData);
    }

    public CountStatsDto build(BaseStudy study) {
      String id = study.getId();
      return CountStatsDto.newBuilder().setVariables(countStatsData.getVariables(id))
          .setStudyVariables(countStatsData.getStudyVariables(id))
          .setDataschemaVariables(countStatsData.getDataschemaVariables(id))
          .setStudyDatasets(countStatsData.getStudyDatasets(id))
          .setHarmonizationDatasets(countStatsData.getHarmonizationDatasets(id))
          .setNetworks(countStatsData.getNetworkCount(id)).build();
    }
  }
}
