/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
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
import java.util.stream.Collectors;

import org.obiba.mica.core.domain.StudyTable;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.web.model.MicaSearch;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

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
      return calculateCounts(dataset.getId(), getStudyIds(dataset));
    }

    private CountStatsDto calculateCounts(String datasetId, List<String> ids) {
      int studies = 0;
      int variables = countStatsData.getVariables(datasetId);

      List<String> networks = Lists.newArrayList();
      for(String id : ids) {
        studies += countStatsData.getStudies(id);
        String network = countStatsData.getNetworksMap(id);
        if (!Strings.isNullOrEmpty(network)) networks.add(network);
      }

      return CountStatsDto.newBuilder().setVariables(variables).setStudies(studies)
        .setNetworks((int) networks.stream().distinct().count()).build();
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
      return calculateCounts(network.getStudyIds());
    }

    private MicaSearch.CountStatsDto calculateCounts(List<String> ids) {
      int variables = 0;
      int studyDatasets = 0;
      int harmonizationDatasets = 0;
      int studies = 0;

      for(String id : ids) {
        variables += countStatsData.getVariables(id);
        studyDatasets += countStatsData.getStudyDatasets(id);
        harmonizationDatasets += countStatsData.getHarmonizationDatasets(id);
        studies += countStatsData.getStudies(id);
      }

      return MicaSearch.CountStatsDto.newBuilder().setVariables(variables).setStudyDatasets(studyDatasets)
          .setHarmonizationDatasets(harmonizationDatasets).setStudies(studies).build();
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
      return MicaSearch.CountStatsDto.newBuilder().setVariables(countStatsData.getVariables(id))
          .setStudyDatasets(countStatsData.getStudyDatasets(id))
          .setHarmonizationDatasets(countStatsData.getHarmonizationDatasets(id))
          .setNetworks(countStatsData.getNetworks(id)).build();
    }

  }

}
