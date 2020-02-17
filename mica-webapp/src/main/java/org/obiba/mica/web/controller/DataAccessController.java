package org.obiba.mica.web.controller;

import com.google.common.collect.Lists;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.DataAccessAmendment;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.domain.DataAccessRequestTimeline;
import org.obiba.mica.access.notification.DataAccessRequestReportNotificationService;
import org.obiba.mica.access.service.DataAccessAmendmentService;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.core.domain.AbstractAuditableDocument;
import org.obiba.mica.core.domain.Comment;
import org.obiba.mica.core.service.CommentsService;
import org.obiba.mica.micaConfig.NoSuchDataAccessFormException;
import org.obiba.mica.micaConfig.domain.DataAccessAmendmentForm;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.service.DataAccessAmendmentFormService;
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
  private DataAccessAmendmentService dataAccessAmendmentService;

  @Inject
  DataAccessFormService dataAccessFormService;

  @Inject
  DataAccessAmendmentFormService dataAccessAmendmentFormService;

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private UserProfileService userProfileService;

  @Inject
  private CommentsService commentsService;

  @Inject
  private DataAccessRequestReportNotificationService dataAccessRequestReportNotificationService;

  @GetMapping("/data-access/{id}")
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

  @GetMapping("/data-access-form/{id}")
  public ModelAndView getForm(@PathVariable String id,
                              @RequestParam(value = "edit", defaultValue = "false") boolean edit,
                              @CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale,
                              @RequestParam(value = "language", required = false) String language) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters(id);
      addDataAccessFormConfiguration(params, getDataAccessRequest(params), !edit, language == null ? locale : language);
      return new ModelAndView("data-access-form", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=data-access-form%2F" + id);
    }
  }

  @GetMapping("/data-access-feasibility/{id}")
  public ModelAndView getFeasibility(@PathVariable String id) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters(id);
      addDataAccessConfiguration(params);
      return new ModelAndView("data-access-feasibility", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=data-access-feasibility%2F" + id);
    }
  }

  @GetMapping("/data-access-history/{id}")
  public ModelAndView getHistory(@PathVariable String id) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters(id);
      DataAccessRequest dar = (DataAccessRequest) params.get("dar");
      addDataAccessConfiguration(params);

      // merge change history from main form and amendment forms
      final List<FormStatusChangeEvent> events = Lists.newArrayList(
        dar.getStatusChangeHistory().stream()
          .map(e -> new FormStatusChangeEvent(userProfileService, dar, e)).collect(Collectors.toList()));
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
      return new ModelAndView("redirect:../signin?redirect=data-access-history%2F" + id);
    }
  }

  @GetMapping("/data-access-amendment-form/{id}")
  public ModelAndView getAmendmentForm(@PathVariable String id,
                                       @RequestParam(value = "edit", defaultValue = "false") boolean edit,
                                       @CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale,
                                       @RequestParam(value = "language", required = false) String language) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newAmendmentParameters(id);
      DataAccessAmendment amendment = getDataAccessAmendment(params);
      addDataAccessAmendmentFormConfiguration(params, amendment, !edit, language == null ? locale : language);

      return new ModelAndView("data-access-amendment-form", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=data-access-amendment-form%2F" + id);
    }
  }

  @GetMapping("/data-access-documents/{id}")
  public ModelAndView getDocuments(@PathVariable String id) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters(id);
      addDataAccessConfiguration(params);
      return new ModelAndView("data-access-documents", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=data-access-documents%2F" + id);
    }
  }

  @GetMapping("/data-access-comments/{id}")
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
      return new ModelAndView("redirect:../signin?redirect=data-access-comments%2F" + id);
    }
  }

  @GetMapping("/data-access-private-comments/{id}")
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
      return new ModelAndView("redirect:../signin?redirect=data-access-private-comments%2F" + id);
    }
  }

  @ExceptionHandler(NoSuchDataAccessRequestException.class)
  public ModelAndView notFoundError(NoSuchDataAccessRequestException ex) {
    ModelAndView model = new ModelAndView("error");
    model.addObject("status", 404);
    model.addObject("msg", ex.getMessage());
    return model;
  }

  //
  // Private methods
  //

  private Map<String, Object> newParameters(String id) {
    checkPermission("/data-access-request", "VIEW", id);
    Map<String, Object> params = newParameters();
    DataAccessRequest dar = getDataAccessRequest(id);
    params.put("dar", dar);
    params.put("pathPrefix", "../..");

    params.put("applicant", userProfileService.getProfileMap(dar.getApplicant(), true));

    List<String> permissions = Lists.newArrayList("VIEW", "EDIT", "DELETE").stream()
      .filter(action -> isPermitted(action, id)).collect(Collectors.toList());
    if (isPermitted("/data-access-request/" + id, "EDIT", "_status"))
      permissions.add("EDIT_STATUS");
    params.put("permissions", permissions);

    List<DataAccessAmendment> amendments = dataAccessAmendmentService.findByParentId(id);
    params.put("amendments", amendments);

    DataAccessAmendment lastAmendment = amendments.stream().max(Comparator.comparing(AbstractAuditableDocument::getLastModifiedDate)).orElse(null);
    params.put("lastAmendment", lastAmendment);

    params.put("commentsCount", commentsService.countPublicComments("/data-access-request", id));
    params.put("privateCommentsCount", commentsService.countPrivateComments("/data-access-request", id));

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

  private boolean isAmendmentPermitted(String action, String id, String amendmentId) {
    return isPermitted("/data-access-request/" + id + "/amendment", action, amendmentId);
  }

  private DataAccessRequest getDataAccessRequest(Map<String, Object> params) {
    return (DataAccessRequest) params.get("dar");
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
    DataAccessForm dataAccessForm = d.get();
    params.put("formConfig", new SchemaFormConfig(micaConfigService, dataAccessForm.getSchema(), dataAccessForm.getDefinition(), request.getContent(), locale, readOnly));
    params.put("accessConfig", new DataAccessConfig(dataAccessForm));
  }

  private void addDataAccessAmendmentFormConfiguration(Map<String, Object> params, DataAccessAmendment amendment, boolean readOnly, String locale) {
    Optional<DataAccessForm> d = dataAccessFormService.find();
    if (!d.isPresent()) throw NoSuchDataAccessFormException.withDefaultMessage();
    Optional<DataAccessAmendmentForm> ad = dataAccessAmendmentFormService.find();
    if (!ad.isPresent()) throw NoSuchDataAccessFormException.withDefaultMessage();
    DataAccessForm dataAccessForm = d.get();
    DataAccessAmendmentForm dataAccessAmendmentForm = ad.get();
    params.put("formConfig", new SchemaFormConfig(micaConfigService, dataAccessAmendmentForm.getSchema(), dataAccessAmendmentForm.getDefinition(), amendment.getContent(), locale, readOnly));
    params.put("accessConfig", new DataAccessConfig(dataAccessForm));
  }

}
