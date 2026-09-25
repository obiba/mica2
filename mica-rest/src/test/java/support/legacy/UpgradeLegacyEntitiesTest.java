/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package support.legacy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UpgradeLegacyEntitiesTest {

  @Test
  public void upgradeStudy_legacy_membership_type() {
    String content = "{\"id\":\"s\",\"memberships\":[{\"role\":\"investigator\",\"members\":[{\"id\":\"p\",\"studyMemberships\":[" +
      "{\"parentId\":\"s\",\"obiba.mica.PersonDto.StudyMembershipDto.meta\":{\"type\":\"harmonization-study\"}}]}]}]}";
    String upgraded = UpgradeLegacyEntities.upgradeStudy(content);
    assertThat(upgraded).contains("\"type\":\"INITIATIVE\"").doesNotContain("StudyMembershipDto.meta");
  }

  @Test
  public void upgradeStudy_current_membership_unchanged() {
    String content = "{\"id\":\"s\",\"memberships\":[{\"role\":\"investigator\",\"members\":[{\"id\":\"p\",\"studyMemberships\":[" +
      "{\"parentId\":\"s\",\"type\":\"STUDY\"}]}]}]}";
    assertThat(UpgradeLegacyEntities.upgradeStudy(content)).isEqualTo(content);
  }
}
