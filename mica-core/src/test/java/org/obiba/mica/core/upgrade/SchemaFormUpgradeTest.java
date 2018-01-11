/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.upgrade;

import org.junit.Test;
import org.obiba.runtime.Version;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SchemaFormUpgradeTest {

  private SchemaFormUpgrade schemaFormUpgrade = new SchemaFormUpgrade();

  @Test
  public void must_be_applied_if_previous_version_is_older_than_v2_and_runtime_version_is_at_least_v2_1_0() {

    assertThat(schemaFormUpgrade.mustBeApplied(from(1, 0, 0), to(2, 0, 0)), is(false));
    assertThat(schemaFormUpgrade.mustBeApplied(from(1, 0, 0), to(2, 1, 0)), is(true));
    assertThat(schemaFormUpgrade.mustBeApplied(from(1, 0, 0), to(2, 2, 0)), is(true));
    assertThat(schemaFormUpgrade.mustBeApplied(from(2, 0, 0), to(2, 2, 0)), is(false));
    assertThat(schemaFormUpgrade.mustBeApplied(from(2, 0, 5), to(2, 2, 0)), is(false));
    assertThat(schemaFormUpgrade.mustBeApplied(from(2, 1, 0), to(2, 2, 0)), is(false));

  }

  private Version from(int major, int minor, int micro) {
    return new Version(major, minor, micro);
  }

  private Version to(int major, int minor, int micro) {
    return from(major, minor, micro);
  }
}
