/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.controller.domain;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.obiba.mica.access.domain.DataAccessCollaborator;

import static org.assertj.core.api.Assertions.assertThat;

public class DataAccessCollaboratorBundleTest {

  private static final String EMAIL = "<img src=x onerror=alert(1)>@x.org";

  private final DataAccessCollaborator collaborator = DataAccessCollaborator.newBuilder("dar1").email(EMAIL).invited().build();

  /**
   * The template prints the full name as is (#4742).
   */
  @Test
  public void test_getFullName_without_profile_encodes_email() {
    DataAccessCollaboratorBundle bundle = new DataAccessCollaboratorBundle(collaborator, Map.of());

    assertThat(bundle.getFullName()).isEqualTo("&lt;img src&#x3d;x onerror&#x3d;alert&#x28;1&#x29;&gt;&#x40;x.org");
    // the template encodes it for the mailto link and the scripts
    assertThat(bundle.getEmail()).isEqualTo(EMAIL);
  }

  /**
   * The profile full name is already encoded by UserProfileService.asMap.
   */
  @Test
  public void test_getFullName_from_profile_is_not_encoded_again() {
    DataAccessCollaboratorBundle bundle = new DataAccessCollaboratorBundle(collaborator, Map.of("fullName", "Tom &amp; Jerry"));

    assertThat(bundle.getFullName()).isEqualTo("Tom &amp; Jerry");
  }

}
