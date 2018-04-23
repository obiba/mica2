package org.obiba.mica.access.domain;


public enum DataAccessEntityStatus {
  OPENED,     // request is being edited by the applicant
  SUBMITTED, // request is submitted by the applicant, ready for review
  REVIEWED,  // request is being reviewed
  CONDITIONALLY_APPROVED,
  APPROVED,  // request was reviewed and approved
  REJECTED   // request was reviewed and rejected
}
