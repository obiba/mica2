package org.obiba.mica.security.domain;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SubjectAclTest {

  @Test
  public void testHasActions() {
    SubjectAcl acl = SubjectAcl.newBuilder("foo", SubjectAcl.Type.USER).build();
    assertFalse(acl.hasActions());
    acl = SubjectAcl.newBuilder("foo", SubjectAcl.Type.USER).action("VIEW","EDIT","DELETE").build();
    assertTrue(acl.hasActions());
  }

  @Test
  public void testHasAction() {
    SubjectAcl acl = SubjectAcl.newBuilder("foo", SubjectAcl.Type.USER).build();
    assertFalse(acl.hasAction("VIEW"));
    acl = SubjectAcl.newBuilder("foo", SubjectAcl.Type.USER).action("VIEW","EDIT","DELETE").build();
    assertTrue(acl.hasAction("VIEW"));
    assertTrue(acl.hasAction("VIEW,DELETE"));
    assertTrue(acl.hasAction("VIEW,PATATE"));
    assertFalse(acl.hasAction("PATATE"));
  }

  @Test
  public void testAddAction() {
    SubjectAcl acl = SubjectAcl.newBuilder("foo", SubjectAcl.Type.USER).build();
    assertFalse(acl.hasAction("VIEW"));
    acl.addAction("VIEW");
    assertTrue(acl.hasAction("VIEW"));
    acl.addAction("EDIT,DELETE");
    assertTrue(acl.hasAction("EDIT"));
    assertTrue(acl.hasAction("DELETE"));
  }

  @Test
  public void testRemoveAction() {
    SubjectAcl acl = SubjectAcl.newBuilder("foo", SubjectAcl.Type.USER).action("VIEW", "EDIT","DELETE").build();
    assertTrue(acl.hasAction("VIEW"));
    acl.removeAction("VIEW");
    assertFalse(acl.hasAction("VIEW"));
    assertTrue(acl.hasAction("EDIT"));
    assertTrue(acl.hasAction("DELETE"));
    acl.removeAction("EDIT,DELETE");
    assertFalse(acl.hasAction("EDIT"));
    assertFalse(acl.hasAction("DELETE"));
    assertFalse(acl.hasActions());
  }
}
