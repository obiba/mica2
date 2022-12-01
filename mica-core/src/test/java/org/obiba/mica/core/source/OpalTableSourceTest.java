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

public class OpalTableSourceTest {

  @Test
  public void test_urn_parse() {
    OpalTableSource source = OpalTableSource.fromURN("urn:opal:MyProject.MyTable");
    assertThat(source.getProject()).isEqualTo("MyProject");
    assertThat(source.getTable()).isEqualTo("MyTable");
  }
}
