package org.obiba.mica.web.controller.domain;

import java.time.LocalDateTime;
import java.util.Map;

import org.obiba.mica.access.domain.DataAccessCollaborator;
import org.owasp.esapi.ESAPI;

public class DataAccessCollaboratorBundle {

  private final DataAccessCollaborator collaborator;

  private final Map<String, Object> profile;

  public DataAccessCollaboratorBundle(DataAccessCollaborator collaborator, Map<String, Object> profile) {
    this.collaborator = collaborator;
    this.profile = profile;
  }

  /**
   * HTML-encoded, as the profile full name: the email is user input.
   */
  public String getFullName() {
    return profile.containsKey("fullName") ? profile.get("fullName").toString() : ESAPI.encoder().encodeForHTML(getEmail());
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
  public LocalDateTime getLastModifiedDate() {
    return collaborator.getLastModifiedDate().orElse(LocalDateTime.now());
  }

  public DataAccessCollaborator getCollaborator() {
    return collaborator;
  }

  public Map<String, Object> getProfile() {
    return profile;
  }
}
