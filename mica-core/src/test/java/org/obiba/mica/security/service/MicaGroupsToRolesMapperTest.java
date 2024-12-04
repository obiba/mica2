package org.obiba.mica.security.service;

import org.junit.Test;
import org.obiba.mica.security.Roles;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
}
