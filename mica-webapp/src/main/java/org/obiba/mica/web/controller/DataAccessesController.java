package org.obiba.mica.web.controller;

import com.google.common.collect.Maps;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.joda.time.DateTime;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.domain.StatusChange;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.access.service.DataAccessRequestUtilService;
import org.obiba.mica.user.UserProfileService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class DataAccessesController extends BaseController {

  @Inject
  private DataAccessRequestService dataAccessRequestService;

  @Inject
  private DataAccessRequestUtilService dataAccessRequestUtilService;

  @Inject
  private UserProfileService userProfileService;

  @GetMapping("/data-accesses")
  public ModelAndView get(@RequestParam(value = "status", required = false) List<String> status) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = Maps.newHashMap();
      List<DataAccessRequestBundle> dars = getDataAccessRequests(status);
      params.put("dars", dars);
      params.put("applicants", dars.stream().map(DataAccessRequestBundle::getApplicant).distinct()
        .collect(Collectors.toMap(u -> u, u -> userProfileService.getProfileMap(u, true))));
      return new ModelAndView("data-accesses", params);
    } else {
      return new ModelAndView("redirect:signin?redirect=data-accesses");
    }
  }

  private List<DataAccessRequestBundle> getDataAccessRequests(List<String> status) {
    return dataAccessRequestService.findByStatus(status).stream() //
      .filter(req -> isPermitted("/data-access-request", "VIEW", req.getId()))
      .map(DataAccessRequestBundle::new)
      .collect(Collectors.toList());
  }

  public class DataAccessRequestBundle {
    private final String id;
    private final DataAccessRequest request;
    private final String title;
    private int totalAmendments = 0;
    private int pendingAmendments = 0;
    private DateTime submitDate;

    public DataAccessRequestBundle(DataAccessRequest request) {
      this.id = request.getId();
      this.request = request;
      this.title = dataAccessRequestUtilService.getRequestTitle(request);
      this.submitDate = request.getSubmissionDate();
    }

    public String getId() {
      return id;
    }

    public String getApplicant() {
      return request.getApplicant();
    }

    public DataAccessRequest getRequest() {
      return request;
    }

    public String getTitle() {
      return title;
    }

    public DateTime getLastUpdate() {
      return request.getLastModifiedDate();
    }

    public DateTime getSubmitDate() {
      return submitDate;
    }

    public DataAccessEntityStatus getStatus() {
      return request.getStatus();
    }

    public int getTotalAmendments() {
      return totalAmendments;
    }

    public int getPendingAmendments() {
      return pendingAmendments;
    }
  }

}
