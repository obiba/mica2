/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access.service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.jayway.jsonpath.*;
import org.apache.shiro.SecurityUtils;
import org.obiba.mica.access.domain.DataAccessAmendment;
import org.obiba.mica.access.domain.DataAccessEntity;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.micaConfig.domain.AbstractDataAccessEntityForm;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.service.DataAccessAmendmentFormService;
import org.obiba.mica.micaConfig.service.DataAccessFormService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.user.UserProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Component
public class DataAccessRequestUtilService {
  private static final Logger log = LoggerFactory.getLogger(DataAccessRequestUtilService.class);

  private static final Configuration conf = Configuration.defaultConfiguration().addOptions(Option.ALWAYS_RETURN_LIST);

  public static final String DEFAULT_NOTIFICATION_SUBJECT = "[${organization}] ${title}";

  private static final SimpleDateFormat ISO_8601 = new SimpleDateFormat("yyyy-MM-dd");

  @Inject
  private DataAccessFormService dataAccessFormService;

  @Inject
  private DataAccessAmendmentFormService dataAccessAmendmentFormService;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private UserProfileService userProfileService;

  public AbstractDataAccessEntityForm getDataAccessForm(boolean isDataAccessRequest) {
    return isDataAccessRequest ? dataAccessFormService.find().get() : dataAccessAmendmentFormService.find().get();
  }

  public String getRequestTitle(DataAccessEntity request) {
    AbstractDataAccessEntityForm dataAccessForm = getDataAccessForm(request instanceof DataAccessRequest);
    return getRequestField(request, dataAccessForm.getTitleFieldPath());
  }

  /**
   * This method merely assigns a value to a path where all parent nodes already exist.
   * <p>
   * TODO create a setRequestField() such that all non-existent nodes are created and proper type checking is made.
   *
   * @param request
   * @param title
   */
  void setRequestTitle(DataAccessEntity request, String title) {
    AbstractDataAccessEntityForm dataAccessForm = getDataAccessForm(request instanceof DataAccessRequest);
    String titleFieldPath = dataAccessForm.getTitleFieldPath();
    if (!Strings.isNullOrEmpty(titleFieldPath)) {
      String rawContent = request.getContent();
      if (!Strings.isNullOrEmpty(titleFieldPath) && !Strings.isNullOrEmpty(rawContent)) {
        Object content = Configuration.defaultConfiguration().jsonProvider().parse(rawContent);
        DocumentContext context = JsonPath.using(conf).parse(content);

        try {
          context.read(titleFieldPath);
          context.set(titleFieldPath, title);
        } catch (PathNotFoundException ex) {
          context.put("$", titleFieldPath.replaceAll("^\\$\\.", ""), title);
        } catch (InvalidPathException e) {
          log.warn("Invalid jsonpath {}", titleFieldPath);
        }

        request.setContent(context.jsonString());
      }
    }
  }

  public String getRequestSummary(DataAccessEntity request) {
    AbstractDataAccessEntityForm dataAccessForm = getDataAccessForm(request instanceof DataAccessRequest);
    return getRequestField(request, dataAccessForm.getSummaryFieldPath());
  }

  public Date getRequestEndDate(DataAccessEntity request) {
    AbstractDataAccessEntityForm dataAccessForm = getDataAccessForm(request instanceof DataAccessRequest);
    if (!dataAccessForm.hasEndDateFieldPath()) return null;
    String value = getRequestField(request, dataAccessForm.getEndDateFieldPath());
    if (Strings.isNullOrEmpty(value)) return null;
    try {
      return ISO_8601.parse(value);
    } catch (ParseException e) {
      log.warn("Not a valid (ISO 8601) date format: {}", value);
      return null;
    }
  }

  public void checkStatusTransition(DataAccessEntity request, DataAccessEntityStatus to)
    throws IllegalArgumentException {
    if (request.getStatus() == to) return;

    switch (request.getStatus()) {
      case OPENED:
        checkOpenedStatusTransition(to);
        break;
      case SUBMITTED:
        checkSubmittedStatusTransition(to);
        break;
      case REVIEWED:
        checkReviewedStatusTransition(to);
        break;
      case CONDITIONALLY_APPROVED:
        checkConditionallyApprovedStatusTransition(to);
        break;
      case APPROVED:
        checkApprovedStatusTransition(to);
        break;
      case REJECTED:
        checkRejectedStatusTransition(to);
        break;
      default:
        throw new IllegalArgumentException("Unexpected data access request status: " + request.getStatus());
    }
  }

  /**
   * Get the possible next status.
   *
   * @return
   */
  public Iterable<DataAccessEntityStatus> nextStatus(DataAccessEntity request) {
    List<DataAccessEntityStatus> to = Lists.newArrayList();
    boolean isPermitted;

    if (request instanceof DataAccessAmendment) {
      isPermitted = subjectAclService.isPermitted("/data-access-request/" + ((DataAccessAmendment) request).getParentId() + "/amendment/" + request.getId(), "EDIT", "_status");
    } else {
      isPermitted = subjectAclService.isPermitted("/data-access-request/" + request.getId(), "EDIT", "_status");
    }

    if (!isPermitted) return to;
    switch (request.getStatus()) {
      case OPENED:
        if (SecurityUtils.getSubject().getPrincipal().toString().equals(request.getApplicant()))
          addNextOpenedStatus(to);
        break;
      case SUBMITTED:
        addNextSubmittedStatus(to);
        break;
      case REVIEWED:
        addNextReviewedStatus(to);
        break;
      case CONDITIONALLY_APPROVED:
        addNextConditionallyApprovedStatus(to);
        break;
      case APPROVED:
        addNextApprovedStatus(to);
        break;
      case REJECTED:
        addNextRejectedStatus(to);
        break;
    }
    return to;
  }

  //
  // Private methods
  //

  private String getRequestField(DataAccessEntity request, String fieldPath) {
    String rawContent = request.getContent();
    if (!Strings.isNullOrEmpty(fieldPath) && !Strings.isNullOrEmpty(rawContent)) {
      Object content = Configuration.defaultConfiguration().jsonProvider().parse(rawContent);
      List<Object> values = null;
      try {
        values = JsonPath.using(conf).parse(content).read(fieldPath);
      } catch (PathNotFoundException ex) {
        //ignore
      } catch (InvalidPathException e) {
        log.warn("Invalid jsonpath {}", fieldPath);
      }

      if (values != null) {
        return values.get(0).toString();
      }
    }
    return null;
  }

  private void addNextOpenedStatus(List<DataAccessEntityStatus> to) {
    to.add(DataAccessEntityStatus.SUBMITTED);
  }

  private void addNextSubmittedStatus(List<DataAccessEntityStatus> to) {
    to.add(DataAccessEntityStatus.OPENED);
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if (dataAccessForm.isWithReview()) {
      to.add(DataAccessEntityStatus.REVIEWED);
    } else {
      to.add(DataAccessEntityStatus.APPROVED);
      to.add(DataAccessEntityStatus.REJECTED);
      if (dataAccessForm.isWithConditionalApproval()) to.add(DataAccessEntityStatus.CONDITIONALLY_APPROVED);
    }
  }

  private void addNextReviewedStatus(List<DataAccessEntityStatus> to) {
    to.add(DataAccessEntityStatus.APPROVED);
    to.add(DataAccessEntityStatus.REJECTED);
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if (dataAccessForm.isWithConditionalApproval()) to.add(DataAccessEntityStatus.CONDITIONALLY_APPROVED);
    else to.add(DataAccessEntityStatus.OPENED);
  }

  private void addNextConditionallyApprovedStatus(List<DataAccessEntityStatus> to) {
    to.add(DataAccessEntityStatus.SUBMITTED);
  }

  private void addNextApprovedStatus(List<DataAccessEntityStatus> to) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if (!dataAccessForm.isApprovedFinal()) {
      if (dataAccessForm.isWithReview()) to.add(DataAccessEntityStatus.REVIEWED);
      else to.add(DataAccessEntityStatus.SUBMITTED);
    }
  }

  private void addNextRejectedStatus(List<DataAccessEntityStatus> to) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if (!dataAccessForm.isRejectedFinal()) {
      if (dataAccessForm.isWithReview()) to.add(DataAccessEntityStatus.REVIEWED);
      else to.add(DataAccessEntityStatus.SUBMITTED);
    }
  }

  private void checkOpenedStatusTransition(DataAccessEntityStatus to) {
    if (to != DataAccessEntityStatus.SUBMITTED)
      throw new IllegalArgumentException("Opened data access request can only be submitted");
  }

  private void checkSubmittedStatusTransition(DataAccessEntityStatus to) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if (dataAccessForm.isWithReview()) {
      if (to != DataAccessEntityStatus.OPENED && to != DataAccessEntityStatus.REVIEWED)
        throw new IllegalArgumentException("Submitted data access request can only be reopened or put under review");
    } else if (!dataAccessForm.isWithReview() && dataAccessForm.isWithConditionalApproval()) {
      if (to != DataAccessEntityStatus.CONDITIONALLY_APPROVED && to != DataAccessEntityStatus.OPENED &&
        to != DataAccessEntityStatus.APPROVED && to != DataAccessEntityStatus.REJECTED)
        throw new IllegalArgumentException("Submitted data access request can only be conditionally approved, reopened, or be approved/rejected");
    } else {
      if (to != DataAccessEntityStatus.OPENED && to != DataAccessEntityStatus.APPROVED &&
        to != DataAccessEntityStatus.REJECTED) throw new IllegalArgumentException(
        "Submitted data access request can only be reopened or be approved/rejected");
    }
  }

  private void checkReviewedStatusTransition(DataAccessEntityStatus to) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if (dataAccessForm.isWithConditionalApproval()) {
      if ((!userProfileService.currentUserIs(Roles.MICA_ADMIN) && to == DataAccessEntityStatus.SUBMITTED)
        && to != DataAccessEntityStatus.CONDITIONALLY_APPROVED
        && to != DataAccessEntityStatus.APPROVED
        && to != DataAccessEntityStatus.REJECTED)
        throw new IllegalArgumentException("Reviewed data access request can only be conditionally approved or be approved/rejected, only the admin can resubmit");
    } else {
      if ((!userProfileService.currentUserIs(Roles.MICA_ADMIN) && to == DataAccessEntityStatus.SUBMITTED)
        && to != DataAccessEntityStatus.OPENED
        && to != DataAccessEntityStatus.APPROVED
        && to != DataAccessEntityStatus.REJECTED)
        throw new IllegalArgumentException("Reviewed data access request can only be reopened or be approved/rejected, only the admin can resubmit");
    }
  }

  private void checkConditionallyApprovedStatusTransition(DataAccessEntityStatus to) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if (dataAccessForm.isWithReview()) {
      if (to != DataAccessEntityStatus.SUBMITTED && to != DataAccessEntityStatus.REVIEWED)
        throw new IllegalArgumentException("Conditionally approved data access request can only be resubmitted or be under review");
    } else {
      if (to != DataAccessEntityStatus.SUBMITTED)
        throw new IllegalArgumentException("Conditionally approved data access request can only be resubmitted");
    }
  }

  private void checkApprovedStatusTransition(DataAccessEntityStatus to) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if (dataAccessForm.isApprovedFinal())
      throw new IllegalArgumentException("Approved data access request cannot be modified");

    if (dataAccessForm.isWithReview() && to != DataAccessEntityStatus.REVIEWED) {
      throw new IllegalArgumentException("Approved data access request can only be put under review");
    }

    if (!dataAccessForm.isWithReview() && to != DataAccessEntityStatus.SUBMITTED) {
      throw new IllegalArgumentException("Approved data access request can only go to submitted state");
    }
  }

  private void checkRejectedStatusTransition(DataAccessEntityStatus to) {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if (dataAccessForm.isApprovedFinal())
      throw new IllegalArgumentException("Rejected data access request cannot be modified");

    if (dataAccessForm.isWithReview() && to != DataAccessEntityStatus.REVIEWED) {
      throw new IllegalArgumentException("Rejected data access request can only be put under review");
    }

    if (!dataAccessForm.isWithReview() && to != DataAccessEntityStatus.SUBMITTED) {
      throw new IllegalArgumentException("Rejected data access request can only go to submitted state");
    }
  }
}
