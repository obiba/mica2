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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class CountStatsData {
  private Map<String, Integer> variables;
  private Map<String, Integer> studyDatasets;
  private Map<String, Integer> harmonizationDatasets;
  private Map<String, Integer> studies;
  private Map<String, Integer> networks;
  private Map<String, List<String>> networksMap;
  private Map<String, Map<String, List<String>>> datasetsMap;

  public static Builder newBuilder() {
    return new Builder();
  }

  public int getVariables(String studyId) {
    return getCount(variables, studyId);
  }

  public int getStudyDatasets(String studyId) {
    return getCount(studyDatasets, studyId);
  }

  public int getHarmonizationDatasets(String studyId) {
    return getCount(harmonizationDatasets, studyId);
  }

  public int getStudies(String studyId) {
    return getCount(studies, studyId);
  }

  public int getNetworks(String studyId) {
    return getCount(networks, studyId);
  }

  public String getNetwork(String studyId) {
    Optional<Map.Entry<String, List<String>>> result = networksMap.entrySet().stream()
      .filter(entry -> entry.getValue().contains(studyId)).findFirst();
    return result.isPresent() ? result.get().getKey() : null;
  }

  private static int getCount(Map<String, Integer> map, String studyId) {
    if(map == null || !map.containsKey(studyId)) {
      return 0;
    }

    return map.get(studyId);
  }

  public Map<String, List<String>> getDataset(String studyId) {
    Map<String, List<String>> map = Maps.newHashMap();
    datasetsMap.entrySet().forEach(
      entry -> {
        Optional<Map.Entry<String, List<String>>> result = entry.getValue().entrySet().stream()
          .filter(subentry -> subentry.getValue().contains(studyId)).findFirst();

        if (result.isPresent()) {
          String key = result.get().getKey();
          List<String> list = map.containsKey(key) ? map.get(key) : Lists.newArrayList();
          list.add(entry.getKey());
          map.put(key, list);
        }
      }
    );

    return map;
  }

  public static class Builder {
    private CountStatsData data = new CountStatsData();

    public Builder variables(Map<String, Integer> value) {
      data.variables = value;
      return this;
    }

    public Builder datasetsMap(Map<String, Map<String, List<String>>> value) {
      data.datasetsMap = value;
      return this;
    }

    public Builder studyDatasets(Map<String, Integer> value) {
      data.studyDatasets = value;
      return this;
    }

    public Builder harmonizationDatasets(Map<String, Integer> value) {
      data.harmonizationDatasets = value;
      return this;
    }

    public Builder studies(Map<String, Integer> value) {
      data.studies = value;
      return this;
    }

    public Builder networks(Map<String, Integer> value) {
      data.networks = value;
      return this;
    }

    public Builder networksMap(Map<String, List<String>> value) {
      data.networksMap = value;
      return this;
    }

    public CountStatsData build() {
      return data;
    }
  }
}
