package org.obiba.mica.web.controller.domain;

import org.obiba.mica.micaConfig.PdfDownloadType;
import org.obiba.mica.micaConfig.domain.DataAccessConfig;
import org.obiba.mica.micaConfig.domain.DataAccessForm;

/**
 * Workflow settings.
 */
public class DataAccessConfigBundle {

  private final DataAccessConfig config;
  private final boolean downloadPdf;

  public DataAccessConfigBundle(DataAccessConfig config, DataAccessForm form) {
    this.config = config;
    this.downloadPdf = PdfDownloadType.Template.equals(form.getPdfDownloadType());
  }

  public boolean isWithReview() {
    return config.isWithReview();
  }

  public boolean isWithConditionalApproval() {
    return config.isWithConditionalApproval();
  }

  public boolean isApprovedFinal() {
    return config.isApprovedFinal();
  }

  public boolean isRejectedFinal() {
    return config.isRejectedFinal();
  }

  public boolean isAmendmentsEnabled() {
    return config.isAmendmentsEnabled();
  }

  public boolean isFeasibilityEnabled() {
    return config.isFeasibilityEnabled();
  }

  public boolean isAgreementEnabled() {
    return config.isAgreementEnabled();
  }

  public boolean isVariablesEnabled() {
    return config.isVariablesEnabled();
  }

  public boolean isAmendmentVariablesEnabled() {
    return config.isAmendmentVariablesEnabled();
  }

  public boolean isFeasibilityVariablesEnabled() {
    return config.isFeasibilityVariablesEnabled();
  }

  public boolean isDownloadPdf() {
    return downloadPdf;
  }
}
