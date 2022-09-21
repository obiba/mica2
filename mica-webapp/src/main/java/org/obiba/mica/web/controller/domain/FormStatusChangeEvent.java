package org.obiba.mica.web.controller.domain;

import org.joda.time.DateTime;
import org.obiba.mica.access.domain.*;
import org.obiba.mica.user.UserProfileService;

import java.util.Map;

import org.obiba.mica.access.domain.DataAccessAmendment;
import org.obiba.mica.access.domain.DataAccessEntity;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.DataAccessFeasibility;
import org.obiba.mica.access.domain.StatusChange;
import org.obiba.mica.user.UserProfileService;

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

  public String getType() {
    return form.getClass().getSimpleName().replaceAll("DataAccess", "").toLowerCase();
  }

  public boolean isAmendment() {
    return form instanceof DataAccessAmendment;
  }

  public boolean isFeasibility() {
    return form instanceof DataAccessFeasibility;
  }

  public boolean isAgreement() {
    return form instanceof DataAccessAgreement;
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

  public LocalDateTime getDate() {
    return change.getChangedOn();
  }
}
