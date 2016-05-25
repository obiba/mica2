/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
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
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.service.DataAccessFormService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Component
public class DataAccessRequestUtilService {
  private static final Logger log = LoggerFactory.getLogger(DataAccessRequestUtilService.class);

  private static final Configuration conf = Configuration.defaultConfiguration().addOptions(Option.ALWAYS_RETURN_LIST);

  public static final String DEFAULT_NOTIFICATION_SUBJECT = "[${organization}] ${title}";

  @Inject
  private DataAccessFormService dataAccessFormService;

  public String getRequestTitle(DataAccessRequest request) {
    DataAccessForm dataAccessForm = dataAccessFormService.findDataAccessForm().get();
    String titleFieldPath = dataAccessForm.getTitleFieldPath();
    String rawContent = request.getContent();
    if(!Strings.isNullOrEmpty(titleFieldPath) && !Strings.isNullOrEmpty(rawContent)) {
      Object content = Configuration.defaultConfiguration().jsonProvider().parse(rawContent);
      List<Object> values = null;
      try {
        values = JsonPath.using(conf).parse(content).read(titleFieldPath);
      } catch(PathNotFoundException ex) {
        //ignore
      } catch(InvalidPathException e) {
        log.warn("Invalid jsonpath {}", titleFieldPath);
      }

      if(values != null) {
        return values.get(0).toString();
      }
    }

    return null;
  }

  public void checkStatusTransition(DataAccessRequest request, DataAccessRequest.Status to)
    throws IllegalArgumentException {
    if(request.getStatus() == to) return;

    switch(request.getStatus()) {
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
  public Iterable<DataAccessRequest.Status> nextStatus(DataAccessRequest request) {
    List<DataAccessRequest.Status> to = Lists.newArrayList();
    switch(request.getStatus()) {
      case OPENED:
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

  private void addNextOpenedStatus(List<DataAccessRequest.Status> to) {
    to.add(DataAccessRequest.Status.SUBMITTED);
  }

  private void addNextSubmittedStatus(List<DataAccessRequest.Status> to) {
    to.add(DataAccessRequest.Status.OPENED);
    DataAccessForm dataAccessForm = dataAccessFormService.findDataAccessForm().get();
    if(dataAccessForm.isWithReview()) {
      to.add(DataAccessRequest.Status.REVIEWED);
    } else {
      to.add(DataAccessRequest.Status.APPROVED);
      to.add(DataAccessRequest.Status.REJECTED);
      if (dataAccessForm.isWithConditionalApproval()) to.add(DataAccessRequest.Status.CONDITIONALLY_APPROVED);
    }
  }

  private void addNextReviewedStatus(List<DataAccessRequest.Status> to) {
    to.add(DataAccessRequest.Status.APPROVED);
    to.add(DataAccessRequest.Status.REJECTED);
    DataAccessForm dataAccessForm = dataAccessFormService.findDataAccessForm().get();
    if (dataAccessForm.isWithConditionalApproval()) to.add(DataAccessRequest.Status.CONDITIONALLY_APPROVED);
     else to.add(DataAccessRequest.Status.OPENED);
  }

  private void addNextConditionallyApprovedStatus(List<DataAccessRequest.Status> to) {
    to.add(DataAccessRequest.Status.SUBMITTED);
  }

  private void addNextApprovedStatus(List<DataAccessRequest.Status> to) {
    DataAccessForm dataAccessForm = dataAccessFormService.findDataAccessForm().get();
    if(!dataAccessForm.isApprovedFinal()) {
      if(dataAccessForm.isWithReview()) to.add(DataAccessRequest.Status.REVIEWED);
      else to.add(DataAccessRequest.Status.SUBMITTED);
    }
  }

  private void addNextRejectedStatus(List<DataAccessRequest.Status> to) {
    DataAccessForm dataAccessForm = dataAccessFormService.findDataAccessForm().get();
    if(!dataAccessForm.isRejectedFinal()) {
      if(dataAccessForm.isWithReview()) to.add(DataAccessRequest.Status.REVIEWED);
      else to.add(DataAccessRequest.Status.SUBMITTED);
    }
  }

  private void checkOpenedStatusTransition(DataAccessRequest.Status to) {
    if(to != DataAccessRequest.Status.SUBMITTED)
      throw new IllegalArgumentException("Opened data access request can only be submitted");
  }

  private void checkSubmittedStatusTransition(DataAccessRequest.Status to) {
    DataAccessForm dataAccessForm = dataAccessFormService.findDataAccessForm().get();
    if(dataAccessForm.isWithReview()) {
      if(to != DataAccessRequest.Status.OPENED && to != DataAccessRequest.Status.REVIEWED)
        throw new IllegalArgumentException("Submitted data access request can only be reopened or put under review");
    } else if (!dataAccessForm.isWithReview() && dataAccessForm.isWithConditionalApproval()) {
      if (to != DataAccessRequest.Status.CONDITIONALLY_APPROVED && to != DataAccessRequest.Status.OPENED &&
        to != DataAccessRequest.Status.APPROVED && to != DataAccessRequest.Status.REJECTED)
        throw new IllegalArgumentException("Submitted data access request can only be conditionally approved, reopened, or be approved/rejected");
    } else {
      if(to != DataAccessRequest.Status.OPENED && to != DataAccessRequest.Status.APPROVED &&
        to != DataAccessRequest.Status.REJECTED) throw new IllegalArgumentException(
        "Submitted data access request can only be reopened or be approved/rejected");
    }
  }

  private void checkReviewedStatusTransition(DataAccessRequest.Status to) {
    DataAccessForm dataAccessForm = dataAccessFormService.findDataAccessForm().get();
    if (dataAccessForm.isWithConditionalApproval()) {
      if (to != DataAccessRequest.Status.CONDITIONALLY_APPROVED && to != DataAccessRequest.Status.APPROVED &&
        to != DataAccessRequest.Status.REJECTED)
        throw new IllegalArgumentException("Reviewed data access request can only be conditionally approved or be approved/rejected");
    } else {
      if(to != DataAccessRequest.Status.OPENED && to != DataAccessRequest.Status.APPROVED &&
        to != DataAccessRequest.Status.REJECTED)
        throw new IllegalArgumentException("Reviewed data access request can only be reopened or be approved/rejected");
    }
  }

  private void checkConditionallyApprovedStatusTransition(DataAccessRequest.Status to) {
    DataAccessForm dataAccessForm = dataAccessFormService.findDataAccessForm().get();
    if (dataAccessForm.isWithReview()) {
      if (to != DataAccessRequest.Status.SUBMITTED && to != DataAccessRequest.Status.REVIEWED)
        throw new IllegalArgumentException("Conditionally approved data access request can only be resubmitted or be under review");
    } else {
      if (to != DataAccessRequest.Status.SUBMITTED)
        throw new IllegalArgumentException("Conditionally approved data access request can only be resubmitted");
    }
  }

  private void checkApprovedStatusTransition(DataAccessRequest.Status to) {
    DataAccessForm dataAccessForm = dataAccessFormService.findDataAccessForm().get();
    if(dataAccessForm.isApprovedFinal())
      throw new IllegalArgumentException("Approved data access request cannot be modified");

    if(dataAccessForm.isWithReview() && to != DataAccessRequest.Status.REVIEWED) {
      throw new IllegalArgumentException("Approved data access request can only be put under review");
    }

    if(!dataAccessForm.isWithReview() && to != DataAccessRequest.Status.SUBMITTED) {
      throw new IllegalArgumentException("Approved data access request can only go to submitted state");
    }
  }

  private void checkRejectedStatusTransition(DataAccessRequest.Status to) {
    DataAccessForm dataAccessForm = dataAccessFormService.findDataAccessForm().get();
    if(dataAccessForm.isApprovedFinal())
      throw new IllegalArgumentException("Rejected data access request cannot be modified");

    if(dataAccessForm.isWithReview() && to != DataAccessRequest.Status.REVIEWED) {
      throw new IllegalArgumentException("Rejected data access request can only be put under review");
    }

    if(!dataAccessForm.isWithReview() && to != DataAccessRequest.Status.SUBMITTED) {
      throw new IllegalArgumentException("Rejected data access request can only go to submitted state");
    }
  }

}
