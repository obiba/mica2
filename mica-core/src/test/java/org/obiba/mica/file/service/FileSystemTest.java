package org.obiba.mica.file.service;

import org.apache.commons.math3.util.Pair;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FileSystemTest {

  @Test
  public void testExtractPathName() {
    Pair<String,String> rval = FileSystemService.extractPathName("population/2/data-collection-event/3/SOP.pdf", null);
    assertThat(rval.getKey()).isEqualTo("population/2/data-collection-event/3");
    assertThat(rval.getValue()).isEqualTo("SOP.pdf");

    rval = FileSystemService.extractPathName("SOP.pdf", null);
    assertThat(rval.getKey()).isEqualTo("");
    assertThat(rval.getValue()).isEqualTo("SOP.pdf");

    rval = FileSystemService.extractPathName("population/2/data-collection-event/3/SOP.pdf", "/study/x");
    assertThat(rval.getKey()).isEqualTo("/study/x/population/2/data-collection-event/3");
    assertThat(rval.getValue()).isEqualTo("SOP.pdf");

    rval = FileSystemService.extractPathName("SOP.pdf", "/study/x");
    assertThat(rval.getKey()).isEqualTo("/study/x");
    assertThat(rval.getValue()).isEqualTo("SOP.pdf");
  }

}
