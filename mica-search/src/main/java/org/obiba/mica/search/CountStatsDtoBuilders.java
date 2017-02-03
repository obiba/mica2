/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.search.queries.DatasetQuery;
import org.obiba.mica.study.domain.Study;

import static org.obiba.mica.web.model.MicaSearch.CountStatsDto;


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
      int studies = 0;
      int variables = countStatsData.getVariables(dataset.getId());

      List<String> networks = Lists.newArrayList();
      for(String id : ids) {
        studies += countStatsData.getStudies(id);
        networks.addAll(countStatsData.getNetworks(id));
      }

      CountStatsDto.Builder builder = CountStatsDto.newBuilder().setVariables(variables) //
        .setStudies(studies);
      int networksCount = dataset instanceof StudyDataset ? (int) networks.stream().distinct().count() :
        ((HarmonizationDataset) dataset).getNetworkId() != null ? 1 : 0;
      builder.setNetworks(networksCount);

      return builder.build();
    }

    private List<String> getStudyIds(Dataset dataset) {
      if(dataset instanceof StudyDataset) {
        StudyDataset sDataset = (StudyDataset) dataset;
        if(sDataset.hasStudyTable()) {
          return Arrays.asList(sDataset.getStudyTable().getStudyId());
        }
      } else {
        HarmonizationDataset hDataset = (HarmonizationDataset) dataset;
        List<StudyTable> tables = hDataset.getStudyTables();
        if(tables.size() > 0) {
          return tables.stream().map(StudyTable::getStudyId).distinct().collect(Collectors.toList());
        }
      }

      return Lists.newArrayList();
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
      return calculateCounts(network.getId(), network.getStudyIds());
    }

    private CountStatsDto calculateCounts(String networkId, List<String> ids) {
      List<String> studyDatasets = Lists.newArrayList();
      List<String> harmonizationDatasets = Lists.newArrayList();

      int studies = 0;

      for(String id : ids) {
        Map<String, List<String>> datasets = countStatsData.getDataset(id);

        if(datasets.containsKey(DatasetQuery.STUDY_JOIN_FIELD)) {
          studyDatasets.addAll(datasets.get(DatasetQuery.STUDY_JOIN_FIELD));
        }

        if(datasets.containsKey(DatasetQuery.HARMONIZATION_JOIN_FIELD)) {
          harmonizationDatasets.addAll(datasets.get(DatasetQuery.HARMONIZATION_JOIN_FIELD));
        }

        studies += countStatsData.getStudies(id);
      }

      harmonizationDatasets = harmonizationDatasets.stream().distinct().collect(Collectors.toList());
      int variables = Sets.union(ImmutableSet.copyOf(studyDatasets), ImmutableSet.copyOf(harmonizationDatasets))
        .stream().mapToInt(countStatsData::getVariables).sum();
      int studyVariables = studyDatasets.stream().mapToInt(countStatsData::getVariables).sum();
      int dataschemaVariables = countStatsData.getNetworkDataschemaVariables(networkId);

      return CountStatsDto.newBuilder().setVariables(variables)
          .setStudyVariables(studyVariables)
          .setDataschemaVariables(dataschemaVariables)
          .setStudyDatasets((int) studyDatasets.stream().distinct().count())
          .setHarmonizationDatasets(countStatsData.getNetworkHarmonizationDatasets(networkId))
          .setStudies(studies)
          .setStudiesWithVariables((int) ids.stream().filter(i -> countStatsData.getDataset(i).containsKey(DatasetQuery.STUDY_JOIN_FIELD)).count())
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

    public CountStatsDto build(Study study) {
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
