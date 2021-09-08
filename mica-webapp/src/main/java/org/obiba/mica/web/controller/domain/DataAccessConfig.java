package org.obiba.mica.web.controller.domain;

import org.obiba.mica.micaConfig.PdfDownloadType;
import org.obiba.mica.micaConfig.domain.DataAccessForm;

/**
 * Workflow settings.
 */
public class DataAccessConfig {

  private final DataAccessForm form;
  private final boolean downloadPdf;

  public DataAccessConfig(DataAccessForm form) {
    this.form = form;
    this.downloadPdf = PdfDownloadType.Template.equals(form.getPdfDownloadType());
  }

  public boolean isWithReview() {
    return form.isWithReview();
  }

  public boolean isWithConditionalApproval() {
    return form.isWithConditionalApproval();
  }

  public boolean isApprovedFinal() {
    return form.isApprovedFinal();
  }

  public boolean isRejectedFinal() {
    return form.isRejectedFinal();
  }

  public boolean isAmendmentsEnabled() {
    return form.isAmendmentsEnabled();
  }

  public boolean isFeasibilityEnabled() {
    return form.isFeasibilityEnabled();
  }

  public boolean isVariablesEnabled() {
    return form.isVariablesEnabled();
  }

  public boolean isAmendmentVariablesEnabled() {
    return form.isAmendmentVariablesEnabled();
  }

  public boolean isFeasibilityVariablesEnabled() {
    return form.isFeasibilityVariablesEnabled();
  }
  public boolean isDownloadPdf() {
    return downloadPdf;
  }
}
