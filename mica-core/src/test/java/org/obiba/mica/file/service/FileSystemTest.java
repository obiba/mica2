/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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

    rval = FileSystemService.extractPathName("population/2/data-collection-event/3/SOP.pdf", "/individual-study/x");
    assertThat(rval.getKey()).isEqualTo("/individual-study/x/population/2/data-collection-event/3");
    assertThat(rval.getValue()).isEqualTo("SOP.pdf");

    rval = FileSystemService.extractPathName("SOP.pdf", "/individual-study/x");
    assertThat(rval.getKey()).isEqualTo("/individual-study/x");
    assertThat(rval.getValue()).isEqualTo("SOP.pdf");
  }

}
