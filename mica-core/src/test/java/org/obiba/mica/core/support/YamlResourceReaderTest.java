package org.obiba.mica.core.support;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class YamlResourceReaderTest {

  @Test
  public void readEmptyExclusionsListYamlTest() {
    Map read = YamlResourceReader.readClassPath("/empty-exclusions-list.yml", Map.class);

    assertTrue(read.containsKey("exclusions"));
    assertTrue(((List) read.get("exclusions")).isEmpty());
  }

  @Test
  public void readExclusionsListYamlTest() {
    Map read = YamlResourceReader.readClassPath("/exclusions-list.yml", Map.class);

    assertTrue(read.containsKey("exclusions"));
    assertNotNull(read.get("exclusions"));
    assertTrue(!((List) read.get("exclusions")).isEmpty());
  }
}
