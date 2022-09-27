package org.obiba.mica.web.controller.domain;

import org.joda.time.DateTime;
import org.obiba.mica.access.domain.DataAccessCollaborator;
import org.obiba.shiro.realm.ObibaRealm;

import java.util.Map;

public class DataAccessCollaboratorBundle {

  private final DataAccessCollaborator collaborator;

  private final Map<String, Object> profile;

  public DataAccessCollaboratorBundle(DataAccessCollaborator collaborator, Map<String, Object> profile) {
    this.collaborator = collaborator;
    this.profile = profile;
  }

  public String getFullName() {
    return profile.containsKey("fullName") ? profile.get("fullName").toString() : getEmail();
  }

  public String getPrincipal() {
    return collaborator.getPrincipal();
  }

  public String getEmail() {
    return collaborator.getEmail();
  }

  public boolean isInvitationPending() {
    return collaborator.isInvitationPending();
  }
  public DateTime getLastModifiedDate() {
    return collaborator.getLastModifiedDate();
  }

  public DataAccessCollaborator getCollaborator() {
    return collaborator;
  }

  public Map<String, Object> getProfile() {
    return profile;
  }
}
