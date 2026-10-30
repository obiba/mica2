/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.user;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.obiba.shiro.realm.ObibaRealm.Subject;

import static org.assertj.core.api.Assertions.assertThat;

public class UserProfileServiceTest {

  private final UserProfileService userProfileService = new UserProfileService();

  /**
   * Profile of an invited collaborator without account: the username is the email typed by the inviter (#4742).
   */
  @Test
  public void test_asMap_encodes_username_full_name() {
    Subject profile = new Subject();
    profile.setUsername("<img src=x onerror=alert(1)>@x.org");

    Map<String, Object> map = userProfileService.asMap(profile);

    assertThat(map.get("fullName")).isEqualTo("&lt;img src&#x3d;x onerror&#x3d;alert&#x28;1&#x29;&gt;&#x40;x.org");
    // compared with other usernames in templates, it stays as is
    assertThat(map.get("username")).isEqualTo("<img src=x onerror=alert(1)>@x.org");
  }

  @Test
  public void test_asMap_encodes_username_full_name_without_last_name() {
    Subject profile = new Subject();
    profile.setUsername("<b>bob</b>");
    profile.setAttributes(List.of(Map.of("key", "firstName", "value", "Bob")));

    assertThat(userProfileService.asMap(profile).get("fullName")).isEqualTo("Bob &lt;b&gt;bob&lt;&#x2f;b&gt;");
  }

  @Test
  public void test_asMap_encodes_username_full_name_without_names() {
    Subject profile = new Subject();
    profile.setUsername("<b>bob</b>");
    profile.setAttributes(List.of(Map.of("key", "email", "value", "bob@example.org")));

    assertThat(userProfileService.asMap(profile).get("fullName")).isEqualTo("&lt;b&gt;bob&lt;&#x2f;b&gt;");
  }

  @Test
  public void test_asMap_last_name_replaces_username() {
    Subject profile = new Subject();
    profile.setUsername("<b>bob</b>");
    profile.setAttributes(List.of(Map.of("key", "lastName", "value", "Smith")));

    assertThat(userProfileService.asMap(profile).get("fullName")).isEqualTo("Smith");
  }

  @Test
  public void test_asMap_full_name_is_encoded_once() {
    Subject profile = new Subject();
    profile.setUsername("bob");
    profile.setAttributes(List.of(
      Map.of("key", "firstName", "value", "Tom & Jerry"),
      Map.of("key", "lastName", "value", "O'Brien")));

    assertThat(userProfileService.asMap(profile).get("fullName")).isEqualTo("Tom &amp; Jerry O&#x27;Brien");
  }

  @Test
  public void test_asMap_plain_username_is_unchanged() {
    Subject profile = new Subject();
    profile.setUsername("bob.smith-1_a");

    assertThat(userProfileService.asMap(profile).get("fullName")).isEqualTo("bob.smith-1_a");
  }

}
