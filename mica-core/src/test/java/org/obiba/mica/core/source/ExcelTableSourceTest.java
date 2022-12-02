/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.source;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExcelTableSourceTest {

  @Test
  public void test_is_for() {
    assertThat(ExcelTableSource.isFor("urn:file:MyProject.xlsx:MyTable")).isTrue();
    assertThat(ExcelTableSource.isFor("urn:file:MyProject.xlsx")).isTrue();
    assertThat(ExcelTableSource.isFor("urn:file:/path/to/MyProject.xlsx:MyTable")).isTrue();
    assertThat(ExcelTableSource.isFor("urn:file:MyProject.xls")).isFalse();
    assertThat(ExcelTableSource.isFor("urn:file:MyProject.spss")).isFalse();
  }

  @Test
  public void test_urn_parse() {
    ExcelTableSource source = ExcelTableSource.fromURN("urn:file:MyProject.xlsx:MyTable");
    assertThat(source.getPath()).isEqualTo("MyProject.xlsx");
    assertThat(source.getTable()).isEqualTo("MyTable");

    source = ExcelTableSource.fromURN("urn:file:MyProject.xlsx");
    assertThat(source.getPath()).isEqualTo("MyProject.xlsx");
    assertThat(source.getTable()).isNullOrEmpty();

    source = ExcelTableSource.fromURN("urn:file:/path/to/MyProject.xlsx:MyTable");
    assertThat(source.getPath()).isEqualTo("/path/to/MyProject.xlsx");
    assertThat(source.getTable()).isEqualTo("MyTable");
  }
}
