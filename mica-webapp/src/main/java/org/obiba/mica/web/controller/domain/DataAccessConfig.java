package org.obiba.mica.web.controller.domain;

import org.obiba.mica.micaConfig.domain.DataAccessForm;

/**
 * Workflow settings.
 */
public class DataAccessConfig {

  private final boolean withReview;
  private final boolean withConditionalApproval;
  private final boolean approvedFinal;
  private final boolean rejectedFinal;
  private final boolean feasibilityEnabled;
  private final boolean amendmentsEnabled;

  public DataAccessConfig(DataAccessForm form) {
    this.withReview = form.isWithReview();
    this.withConditionalApproval = form.isWithConditionalApproval();
    this.approvedFinal = form.isApprovedFinal();
    this.rejectedFinal = form.isRejectedFinal();
    this.feasibilityEnabled = form.isFeasibilityEnabled();
    this.amendmentsEnabled = form.isAmendmentsEnabled();
  }

  public boolean isWithReview() {
    return withReview;
  }

  public boolean isWithConditionalApproval() {
    return withConditionalApproval;
  }

  public boolean isApprovedFinal() {
    return approvedFinal;
  }

  public boolean isRejectedFinal() {
    return rejectedFinal;
  }

  public boolean isAmendmentsEnabled() {
    return amendmentsEnabled;
  }

  public boolean isFeasibilityEnabled() {
    return feasibilityEnabled;
  }
}
