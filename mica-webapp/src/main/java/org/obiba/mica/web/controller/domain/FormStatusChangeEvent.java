package org.obiba.mica.web.controller.domain;

import org.joda.time.DateTime;
import org.obiba.mica.access.domain.*;
import org.obiba.mica.user.UserProfileService;
import org.obiba.mica.web.controller.DataAccessController;

import java.util.Map;

public class FormStatusChangeEvent {

  private final UserProfileService userProfileService;
  private final DataAccessEntity form;
  private final StatusChange change;

  public FormStatusChangeEvent(UserProfileService userProfileService, DataAccessEntity form, StatusChange change) {
    this.userProfileService = userProfileService;
    this.form = form;
    this.change = change;
  }

  public DataAccessEntity getForm() {
    return form;
  }

  public boolean isAmendment() {
    return form instanceof DataAccessAmendment;
  }

  public boolean isFeasibility() {
    return form instanceof DataAccessFeasibility;
  }

  public DataAccessEntityStatus getStatus() {
    return change.getTo();
  }

  public String getAuthor() {
    return change.getAuthor();
  }

  public Map<String, Object> getProfile() {
    return userProfileService.getProfileMap(change.getAuthor(), true);
  }

  public DateTime getDate() {
    return change.getChangedOn();
  }
}
