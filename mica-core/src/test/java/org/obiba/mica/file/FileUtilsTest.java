/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FileUtilsTest {

  @Test
  public void testEncode() {
    assertThat(FileUtils.encode("/toto/tutu/some silly, file: path.pdf")).isEqualTo("/toto/tutu/some+silly%2C+file%3A+path.pdf");
    assertThat(FileUtils.encode(null)).isNull();
  }

  @Test
  public void testNormalizeRegex() {
    assertThat(FileUtils.normalizeRegex("/toto/tutu/Case Report (CRF)")).isEqualTo
        ("/toto/tutu/Case Report \\(CRF\\)");
    assertThat(FileUtils.decode(null)).isNull();
  }

  @Test
  public void testDecode() {
    assertThat(FileUtils.decode("/toto/tutu/some+silly%2C+file%3A+path.pdf")).isEqualTo
      ("/toto/tutu/some silly, file: path.pdf");
    assertThat(FileUtils.decode(null)).isNull();
  }
}
