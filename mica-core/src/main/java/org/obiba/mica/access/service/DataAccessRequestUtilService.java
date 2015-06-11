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

import java.util.List;

import javax.inject.Inject;

import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.service.DataAccessFormService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;

@Component
public class DataAccessRequestUtilService {
  private static final Logger log = LoggerFactory.getLogger(DataAccessRequestUtilService.class);

  private static final Configuration conf = Configuration.defaultConfiguration().addOptions(Option.ALWAYS_RETURN_LIST);

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

  @SuppressWarnings("OverlyLongMethod")
  public void checkStatusTransition(DataAccessRequest request, DataAccessRequest.Status to)
    throws IllegalArgumentException {
    if(request.getStatus() == to) return;

    switch(request.getStatus()) {
      case OPENED:
        if(to != DataAccessRequest.Status.SUBMITTED)
          throw new IllegalArgumentException("Opened data access request can only be submitted");
        break;
      case SUBMITTED:
        DataAccessForm dataAccessForm = dataAccessFormService.findDataAccessForm().get();
        if(dataAccessForm.isWithReview()) {
          if(to != DataAccessRequest.Status.OPENED && to != DataAccessRequest.Status.REVIEWED)
            throw new IllegalArgumentException(
              "Submitted data access request can only be reopened or put under review");
        } else {
          if(to != DataAccessRequest.Status.OPENED && to != DataAccessRequest.Status.APPROVED &&
            to != DataAccessRequest.Status.REJECTED) throw new IllegalArgumentException(
            "Submitted data access request can only be reopened or be approved/rejected");
        }
        break;
      case REVIEWED:
        if(to != DataAccessRequest.Status.OPENED && to != DataAccessRequest.Status.APPROVED &&
          to != DataAccessRequest.Status.REJECTED) throw new IllegalArgumentException(
          "Reviewed data access request can only be reopened or be approved/rejected");
        break;
      case APPROVED:
        throw new IllegalArgumentException("Approved data access request cannot be modified");
      case REJECTED:
        throw new IllegalArgumentException("Rejected data access request cannot be modified");
      default:
        throw new IllegalArgumentException("Unexpected data access request status: " + request.getStatus());
    }
  }

  /**
   * Get the possible next status.
   *
   * @return
   */
  @SuppressWarnings("OverlyLongMethod")
  public Iterable<DataAccessRequest.Status> nextStatus(DataAccessRequest request) {
    List<DataAccessRequest.Status> to = Lists.newArrayList();
    switch(request.getStatus()) {
      case OPENED:
        to.add(DataAccessRequest.Status.SUBMITTED);
        break;
      case SUBMITTED:
        to.add(DataAccessRequest.Status.OPENED);
        DataAccessForm dataAccessForm = dataAccessFormService.findDataAccessForm().get();
        if(dataAccessForm.isWithReview()) {
          to.add(DataAccessRequest.Status.REVIEWED);
        } else {
          to.add(DataAccessRequest.Status.APPROVED);
          to.add(DataAccessRequest.Status.REJECTED);
        }
        break;
      case REVIEWED:
        to.add(DataAccessRequest.Status.OPENED);
        to.add(DataAccessRequest.Status.APPROVED);
        to.add(DataAccessRequest.Status.REJECTED);
        break;
      case APPROVED:
      case REJECTED:
        // final state
        break;
    }
    return to;
  }
}
