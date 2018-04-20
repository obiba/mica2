package org.obiba.mica.core.support;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class YamlClassPathResourceReaderTest {

  @Test
  public void readEmptyExclusionsListYamlTest() {
    Map read = YamlClassPathResourceReader.read("/empty-exclusions-list.yml", Map.class);

    assertTrue(read.containsKey("exclusions"));
    assertTrue(((List) read.get("exclusions")).isEmpty());
  }

  @Test
  public void readExclusionsListYamlTest() {
    Map read = YamlClassPathResourceReader.read("/exclusions-list.yml", Map.class);

    assertTrue(read.containsKey("exclusions"));
    assertNotNull(read.get("exclusions"));
    assertTrue(!((List) read.get("exclusions")).isEmpty());
  }
}
