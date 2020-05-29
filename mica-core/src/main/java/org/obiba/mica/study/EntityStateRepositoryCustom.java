package org.obiba.mica.study;

import java.util.LinkedHashMap;
import java.util.List;

public interface EntityStateRepositoryCustom {
  List<LinkedHashMap> countByEachStateStatus();
  List<LinkedHashMap> createEmptyCountByEachStateStatus();
}
