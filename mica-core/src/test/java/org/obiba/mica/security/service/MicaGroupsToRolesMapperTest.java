package org.obiba.mica.security.service;

import org.junit.jupiter.api.Test;
import org.obiba.mica.security.Roles;

import org.springframework.mock.env.MockEnvironment;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MicaGroupsToRolesMapperTest {


  @Test
  public void testSingleGroup() {
    MicaGroupsToRolesMapper mapper = new MicaGroupsToRolesMapper("local-administrator", Roles.MICA_ADMIN);
    assertTrue(mapper.toRoles(Set.of("local-administrator")).contains(Roles.MICA_ADMIN));
    assertFalse(mapper.toRoles(Set.of("some-group")).contains(Roles.MICA_ADMIN));
  }

  @Test
  public void testMultipleGroups() {
    MicaGroupsToRolesMapper mapper = new MicaGroupsToRolesMapper("opal-administrator,mica-user", Roles.MICA_ADMIN);
    assertTrue(mapper.toRoles(Set.of("opal-administrator", "mica-user")).contains(Roles.MICA_ADMIN));
    assertTrue(mapper.toRoles(Set.of("opal-administrator", "mica-user", "some-group")).contains(Roles.MICA_ADMIN));
    assertFalse(mapper.toRoles(Set.of("opal-administrator")).contains(Roles.MICA_ADMIN));
    assertFalse(mapper.toRoles(Set.of("mica-user")).contains(Roles.MICA_ADMIN));
    assertTrue(mapper.toRoles(Set.of("mica-user")).contains(Roles.MICA_USER));
    assertFalse(mapper.toRoles(Set.of("some-group")).contains(Roles.MICA_ADMIN));
  }

  @Test
  public void testMultipleOptionsGroups() {
    MicaGroupsToRolesMapper mapper = new MicaGroupsToRolesMapper("opal-administrator,mica-user|local-administrator", Roles.MICA_ADMIN);
    assertTrue(mapper.toRoles(Set.of("opal-administrator", "mica-user")).contains(Roles.MICA_ADMIN));
    assertTrue(mapper.toRoles(Set.of("opal-administrator", "mica-user", "some-group")).contains(Roles.MICA_ADMIN));
    assertTrue(mapper.toRoles(Set.of("local-administrator")).contains(Roles.MICA_ADMIN));
    assertFalse(mapper.toRoles(Set.of("opal-administrator")).contains(Roles.MICA_ADMIN));
    assertFalse(mapper.toRoles(Set.of("mica-user")).contains(Roles.MICA_ADMIN));
    assertTrue(mapper.toRoles(Set.of("mica-user")).contains(Roles.MICA_USER));
    assertFalse(mapper.toRoles(Set.of("some-group")).contains(Roles.MICA_ADMIN));
  }

  @Test
  public void testDefaults() {
    MicaGroupsToRolesMapper mapper = new MicaGroupsToRolesMapper("", Roles.MICA_ADMIN);
    assertTrue(mapper.toRoles(Set.of(Roles.MICA_USER)).contains(Roles.MICA_USER));
    assertTrue(mapper.toRoles(Set.of(Roles.MICA_DAO)).contains(Roles.MICA_DAO));
    assertTrue(mapper.toRoles(Set.of(Roles.MICA_EDITOR)).contains(Roles.MICA_EDITOR));
    assertTrue(mapper.toRoles(Set.of(Roles.MICA_REVIEWER)).contains(Roles.MICA_REVIEWER));
    assertTrue(mapper.toRoles(Set.of(Roles.MICA_ADMIN)).contains(Roles.MICA_ADMIN));
  }

  @Test
  public void testExternalEditorIsMappable() {
    MicaGroupsToRolesMapper mapper = new MicaGroupsToRolesMapper("external-a", Roles.MICA_EXTERNAL_EDITOR);
    assertTrue(mapper.toRoles(Set.of("external-a")).contains(Roles.MICA_EXTERNAL_EDITOR));
  }

  @Test
  public void testToGroupsSingleGroup() {
    MicaGroupsToRolesMapper mapper = new MicaGroupsToRolesMapper("mica-reviewer-a", Roles.MICA_REVIEWER);
    assertEquals(Set.of("mica-reviewer-a"), mapper.toGroups(Roles.MICA_REVIEWER));
  }

  @Test
  public void testToGroupsMultipleGroups() {
    MicaGroupsToRolesMapper mapper = new MicaGroupsToRolesMapper("opal-administrator,mica-user", Roles.MICA_ADMIN);
    assertEquals(Set.of("opal-administrator", "mica-user"), mapper.toGroups(Roles.MICA_ADMIN));
  }

  @Test
  public void testToGroupsMultipleOptionsGroups() {
    MicaGroupsToRolesMapper mapper = new MicaGroupsToRolesMapper("opal-administrator,mica-user|local-administrator", Roles.MICA_ADMIN);
    assertEquals(Set.of("opal-administrator", "mica-user", "local-administrator"), mapper.toGroups(Roles.MICA_ADMIN));
  }

  @Test
  public void testToGroupsUnconfigured() {
    MicaGroupsToRolesMapper mapper = new MicaGroupsToRolesMapper("", Roles.MICA_ADMIN);
    assertEquals(Set.of(Roles.MICA_ADMIN), mapper.toGroups(Roles.MICA_ADMIN));
    assertEquals(Set.of(Roles.MICA_DAO), mapper.toGroups(Roles.MICA_DAO));
    // not a role: a plain group name
    assertEquals(Set.of("some-group"), mapper.toGroups("some-group"));
  }

  @Test
  public void testToGroupsMultipleRoles() {
    MicaGroupsToRolesMapper mapper = new MicaGroupsToRolesMapper("mica-dao-a", Roles.MICA_DAO);
    assertEquals(Set.of(Roles.MICA_ADMIN, "mica-dao-a", "some-group"),
      mapper.toGroups(Roles.MICA_ADMIN, Roles.MICA_DAO, "some-group"));
  }

  @Test
  public void testEnvironment() {
    MockEnvironment environment = new MockEnvironment()
      .withProperty("roles.mica-reviewer", "mica-reviewer-a")
      .withProperty("roles.mica-editor", " mica-editor-a | mica-editor-b ");
    MicaGroupsToRolesMapper mapper = new MicaGroupsToRolesMapper(environment);
    Set<String> roles = mapper.toRoles(Set.of("mica-reviewer-a"));
    assertTrue(roles.contains(Roles.MICA_REVIEWER));
    assertTrue(roles.contains("mica-reviewer-a")); // raw group kept as a principal
    assertEquals(Set.of("mica-reviewer-a"), mapper.toGroups(Roles.MICA_REVIEWER));
    assertEquals(Set.of("mica-editor-a", "mica-editor-b"), mapper.toGroups(Roles.MICA_EDITOR));
    // unconfigured roles keep their defaults
    assertTrue(mapper.toRoles(Set.of(Roles.MICA_ADMIN)).contains(Roles.MICA_ADMIN));
    assertEquals(Set.of(Roles.MICA_DAO), mapper.toGroups(Roles.MICA_DAO));
  }
}
