package org.obiba.mica.web.controller;

import com.google.common.collect.Lists;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.DataAccessAmendment;
import org.obiba.mica.access.domain.DataAccessFeasibility;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.domain.DataAccessRequestTimeline;
import org.obiba.mica.access.notification.DataAccessRequestReportNotificationService;
import org.obiba.mica.access.service.DataAccessAmendmentService;
import org.obiba.mica.access.service.DataAccessFeasibilityService;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.core.domain.AbstractAuditableDocument;
import org.obiba.mica.core.domain.Comment;
import org.obiba.mica.core.service.CommentsService;
import org.obiba.mica.micaConfig.NoSuchDataAccessFormException;
import org.obiba.mica.micaConfig.domain.DataAccessAmendmentForm;
import org.obiba.mica.micaConfig.domain.DataAccessFeasibilityForm;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.service.DataAccessAmendmentFormService;
import org.obiba.mica.micaConfig.service.DataAccessFeasibilityFormService;
import org.obiba.mica.micaConfig.service.DataAccessFormService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.user.UserProfileService;
import org.obiba.mica.web.controller.domain.DataAccessConfig;
import org.obiba.mica.web.controller.domain.FormStatusChangeEvent;
import org.obiba.mica.web.controller.domain.SchemaFormConfig;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class DataAccessController extends BaseController {

  @Inject
  private DataAccessRequestService dataAccessRequestService;

  @Inject
  private DataAccessFeasibilityService dataAccessFeasibilityService;

  @Inject
  private DataAccessAmendmentService dataAccessAmendmentService;

  @Inject
  private DataAccessFormService dataAccessFormService;

  @Inject
  private DataAccessAmendmentFormService dataAccessAmendmentFormService;

  @Inject
  private DataAccessFeasibilityFormService dataAccessFeasibilityFormService;

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private UserProfileService userProfileService;

  @Inject
  private CommentsService commentsService;

  @Inject
  private DataAccessRequestReportNotificationService dataAccessRequestReportNotificationService;

  @GetMapping("/data-access/{id:.+}")
  public ModelAndView get(@PathVariable String id) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters(id);
      addDataAccessConfiguration(params);

      DataAccessRequestTimeline timeline = dataAccessRequestReportNotificationService.getReportsTimeline(getDataAccessRequest(params));
      params.put("reportTimeline", timeline);

      return new ModelAndView("data-access", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=data-access%2F" + id);
    }
  }

  @GetMapping("/data-access-form/{id:.+}")
  public ModelAndView getForm(@PathVariable String id,
                              @RequestParam(value = "edit", defaultValue = "false") boolean edit,
                              @CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale,
                              @RequestParam(value = "language", required = false) String language) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters(id);
      addDataAccessFormConfiguration(params, getDataAccessRequest(params), !edit, getLang(locale, language));
      return new ModelAndView("data-access-form", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=/data-access-form%2F" + id);
    }
  }

  @GetMapping("/data-access-history/{id:.+}")
  public ModelAndView getHistory(@PathVariable String id) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters(id);
      DataAccessRequest dar = (DataAccessRequest) params.get("dar");
      addDataAccessConfiguration(params);

      // merge change history from main form, feasibility and amendment forms
      final List<FormStatusChangeEvent> events = Lists.newArrayList(
        dar.getStatusChangeHistory().stream()
          .map(e -> new FormStatusChangeEvent(userProfileService, dar, e)).collect(Collectors.toList()));
      getDataAccessFeasibilities(params).forEach(a -> {
        a.getStatusChangeHistory().stream().map(e -> new FormStatusChangeEvent(userProfileService, a, e))
          .forEach(events::add);
      });
      getDataAccessAmendments(params).forEach(a -> {
        a.getStatusChangeHistory().stream().map(e -> new FormStatusChangeEvent(userProfileService, a, e))
          .forEach(events::add);
      });

      // order change events
      params.put("statusChangeEvents", events.stream().sorted(new Comparator<FormStatusChangeEvent>() {
        @Override
        public int compare(FormStatusChangeEvent event1, FormStatusChangeEvent event2) {
          return event1.getDate().compareTo(event2.getDate());
        }
      }).collect(Collectors.toList()));

      return new ModelAndView("data-access-history", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=/data-access-history%2F" + id);
    }
  }

  @GetMapping("/data-access-feasibility-form/{id:.+}")
  public ModelAndView getFeasibilityForm(@PathVariable String id,
                                       @RequestParam(value = "edit", defaultValue = "false") boolean edit,
                                       @CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale,
                                       @RequestParam(value = "language", required = false) String language) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newFeasibilityParameters(id);
      DataAccessFeasibility feasibility = getDataAccessFeasibility(params);
      addDataAccessFeasibilityFormConfiguration(params, feasibility, !edit, getLang(locale, language));

      return new ModelAndView("data-access-feasibility-form", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=/data-access-feasibility-form%2F" + id);
    }
  }

  @GetMapping("/data-access-amendment-form/{id:.+}")
  public ModelAndView getAmendmentForm(@PathVariable String id,
                                       @RequestParam(value = "edit", defaultValue = "false") boolean edit,
                                       @CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale,
                                       @RequestParam(value = "language", required = false) String language) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newAmendmentParameters(id);
      DataAccessAmendment amendment = getDataAccessAmendment(params);
      addDataAccessAmendmentFormConfiguration(params, amendment, !edit, getLang(locale, language));

      return new ModelAndView("data-access-amendment-form", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=/data-access-amendment-form%2F" + id);
    }
  }

  @GetMapping("/data-access-documents/{id:.+}")
  public ModelAndView getDocuments(@PathVariable String id) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters(id);
      addDataAccessConfiguration(params);
      return new ModelAndView("data-access-documents", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=/data-access-documents%2F" + id);
    }
  }

  @GetMapping("/data-access-comments/{id:.+}")
  public ModelAndView getComments(@PathVariable String id) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters(id);
      addDataAccessConfiguration(params);

      List<Comment> comments = commentsService.findPublicComments("/data-access-request", id);
      params.put("comments", comments);
      params.put("authors", comments.stream().map(AbstractAuditableDocument::getCreatedBy).distinct()
        .collect(Collectors.toMap(u -> u, u -> userProfileService.getProfileMap(u, true))));

      return new ModelAndView("data-access-comments", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=/data-access-comments%2F" + id);
    }
  }

  @GetMapping("/data-access-private-comments/{id:.+}")
  public ModelAndView getPrivateComments(@PathVariable String id) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters(id);
      addDataAccessConfiguration(params);

      if (!isPermitted("/data-access-request/private-comment", "VIEW"))
        checkPermission("/private-comment/data-access-request", "VIEW", null);

      List<Comment> comments = commentsService.findPrivateComments("/data-access-request", id);
      params.put("comments", comments);
      params.put("authors", comments.stream().map(AbstractAuditableDocument::getCreatedBy).distinct()
        .collect(Collectors.toMap(u -> u, u -> userProfileService.getProfileMap(u, true))));

      return new ModelAndView("data-access-private-comments", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=/data-access-private-comments%2F" + id);
    }
  }

  @ExceptionHandler(NoSuchDataAccessRequestException.class)
  public ModelAndView notFoundError(NoSuchDataAccessRequestException ex) {
    return makeErrorModelAndView(404, ex.getMessage());
  }

  //
  // Private methods
  //

  private ModelAndView makeErrorModelAndView(int status, String message) {
    ModelAndView model = new ModelAndView("error");
    model.addObject("status", status);
    model.addObject("msg", message);
    return model;
  }

  private Map<String, Object> newParameters(String id) {
    checkPermission("/data-access-request", "VIEW", id);
    Map<String, Object> params = newParameters();
    DataAccessRequest dar = getDataAccessRequest(id);
    params.put("dar", dar);
    params.put("applicant", userProfileService.getProfileMap(dar.getApplicant(), true));

    List<String> permissions = Lists.newArrayList("VIEW", "EDIT", "DELETE").stream()
      .filter(action -> isPermitted(action, id)).collect(Collectors.toList());
    if (isPermitted("/data-access-request/" + id, "EDIT", "_status"))
      permissions.add("EDIT_STATUS");
    params.put("permissions", permissions);

    List<DataAccessFeasibility> feasibilities = dataAccessFeasibilityService.findByParentId(id);
    params.put("feasibilities", feasibilities);

    DataAccessFeasibility lastFeasibility = feasibilities.stream().max(Comparator.comparing(AbstractAuditableDocument::getLastModifiedDate)).orElse(null);
    params.put("lastFeasibility", lastFeasibility);

    List<DataAccessAmendment> amendments = dataAccessAmendmentService.findByParentId(id);
    params.put("amendments", amendments);

    DataAccessAmendment lastAmendment = amendments.stream().max(Comparator.comparing(AbstractAuditableDocument::getLastModifiedDate)).orElse(null);
    params.put("lastAmendment", lastAmendment);

    params.put("commentsCount", commentsService.countPublicComments("/data-access-request", id));
    params.put("privateCommentsCount", commentsService.countPrivateComments("/data-access-request", id));

    return params;
  }

  private Map<String, Object> newFeasibilityParameters(String id) {
    DataAccessFeasibility feasibility = dataAccessFeasibilityService.findById(id);
    Map<String, Object> params = newParameters(feasibility.getParentId());
    params.put("feasibility", feasibility);

    List<String> permissions = Lists.newArrayList("VIEW", "EDIT", "DELETE").stream()
      .filter(action -> isFeasibilityPermitted(action,feasibility.getParentId(), id)).collect(Collectors.toList());
    if (isPermitted("/data-access-request/" + feasibility.getParentId() + "/feasibility/" + id, "EDIT", "_status"))
      permissions.add("EDIT_STATUS");
    params.put("feasibilityPermissions", permissions);

    return params;
  }

  private Map<String, Object> newAmendmentParameters(String id) {
    DataAccessAmendment amendment = dataAccessAmendmentService.findById(id);
    Map<String, Object> params = newParameters(amendment.getParentId());
    params.put("amendment", amendment);

    List<String> permissions = Lists.newArrayList("VIEW", "EDIT", "DELETE").stream()
      .filter(action -> isAmendmentPermitted(action, amendment.getParentId(), id)).collect(Collectors.toList());
    if (isPermitted("/data-access-request/" + amendment.getParentId() + "/amendment/" + id, "EDIT", "_status"))
      permissions.add("EDIT_STATUS");
    params.put("amendmentPermissions", permissions);

    return params;
  }

  private boolean isPermitted(String action, String id) {
    return isPermitted("/data-access-request", action, id);
  }

  private boolean isFeasibilityPermitted(String action, String id, String feasibilityId) {
    return isPermitted("/data-access-request/" + id + "/feasibility", action, feasibilityId);
  }

  private boolean isAmendmentPermitted(String action, String id, String amendmentId) {
    return isPermitted("/data-access-request/" + id + "/amendment", action, amendmentId);
  }

  private DataAccessRequest getDataAccessRequest(Map<String, Object> params) {
    return (DataAccessRequest) params.get("dar");
  }

  private DataAccessFeasibility getDataAccessFeasibility(Map<String, Object> params) {
    return (DataAccessFeasibility) params.get("feasibility");
  }

  private List<DataAccessFeasibility> getDataAccessFeasibilities(Map<String, Object> params) {
    return (List<DataAccessFeasibility>) params.get("feasibilities");
  }

  private DataAccessAmendment getDataAccessAmendment(Map<String, Object> params) {
    return (DataAccessAmendment) params.get("amendment");
  }

  private List<DataAccessAmendment> getDataAccessAmendments(Map<String, Object> params) {
    return (List<DataAccessAmendment>) params.get("amendments");
  }

  private DataAccessRequest getDataAccessRequest(String id) {
    return dataAccessRequestService.findById(id);
  }

  private void addDataAccessConfiguration(Map<String, Object> params) {
    Optional<DataAccessForm> d = dataAccessFormService.find();
    if (!d.isPresent()) throw NoSuchDataAccessFormException.withDefaultMessage();
    DataAccessForm dataAccessForm = d.get();
    params.put("accessConfig", new DataAccessConfig(dataAccessForm));
  }

  private void addDataAccessFormConfiguration(Map<String, Object> params, DataAccessRequest request, boolean readOnly, String locale) {
    Optional<DataAccessForm> d = dataAccessFormService.find();
    if (!d.isPresent()) throw NoSuchDataAccessFormException.withDefaultMessage();
    DataAccessForm dataAccessForm = getDataAccessForm();
    params.put("formConfig", new SchemaFormConfig(micaConfigService, dataAccessForm.getSchema(), dataAccessForm.getDefinition(), request.getContent(), locale, readOnly));
    params.put("accessConfig", new DataAccessConfig(dataAccessForm));
  }

  private void addDataAccessAmendmentFormConfiguration(Map<String, Object> params, DataAccessAmendment amendment, boolean readOnly, String locale) {
    Optional<DataAccessAmendmentForm> ad = dataAccessAmendmentFormService.find();
    if (!ad.isPresent()) throw NoSuchDataAccessFormException.withDefaultMessage();
    DataAccessAmendmentForm dataAccessAmendmentForm = ad.get();
    params.put("formConfig", new SchemaFormConfig(micaConfigService, dataAccessAmendmentForm.getSchema(), dataAccessAmendmentForm.getDefinition(), amendment.getContent(), locale, readOnly));
    params.put("accessConfig", new DataAccessConfig(getDataAccessForm()));
  }

  private void addDataAccessFeasibilityFormConfiguration(Map<String, Object> params, DataAccessFeasibility feasibility, boolean readOnly, String locale) {
    Optional<DataAccessFeasibilityForm> fd = dataAccessFeasibilityFormService.find();
    if (!fd.isPresent()) throw NoSuchDataAccessFormException.withDefaultMessage();
    DataAccessFeasibilityForm dataAccessFeasibilityForm = fd.get();
    params.put("formConfig", new SchemaFormConfig(micaConfigService, dataAccessFeasibilityForm.getSchema(), dataAccessFeasibilityForm.getDefinition(), feasibility.getContent(), locale, readOnly));
    params.put("accessConfig", new DataAccessConfig(getDataAccessForm()));
  }

  private DataAccessForm getDataAccessForm() {
    Optional<DataAccessForm> d = dataAccessFormService.find();
    if (!d.isPresent()) throw NoSuchDataAccessFormException.withDefaultMessage();
    return d.get();
  }

  private DataAccessConfig getDataAccessConfig(Map<String, Object> params) {
    return (DataAccessConfig) params.get("accessConfig");
  }

}
