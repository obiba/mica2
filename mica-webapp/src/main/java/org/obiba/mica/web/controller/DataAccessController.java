package org.obiba.mica.web.controller;

import com.google.common.collect.Lists;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.obiba.core.translator.JsonTranslator;
import org.obiba.core.translator.PrefixedValueTranslator;
import org.obiba.core.translator.TranslationUtils;
import org.obiba.core.translator.Translator;
import org.obiba.mica.access.domain.ChangeLog;
import org.obiba.mica.access.domain.DataAccessAmendment;
import org.obiba.mica.access.domain.DataAccessRequest;
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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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

  @GetMapping("/data-access/{id}")
  public ModelAndView get(@PathVariable String id) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters(id);
      addDataAccessConfiguration(params);
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

      params.put("authors", dar.getStatusChangeHistory().stream().map(ChangeLog::getAuthor).distinct()
        .collect(Collectors.toMap(u -> u, u -> userProfileService.getProfileMap(u, true))));

      return new ModelAndView("data-access-history", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=data-access-history%2F" + id);
    }
  }

  @GetMapping("/data-access-amendment-form/{id}")
  public ModelAndView getAmendmentForm(@PathVariable String id, @RequestParam("id") String amendmentId,
                                       @RequestParam(value = "edit", defaultValue = "false") boolean edit,
                                       @CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale,
                                       @RequestParam(value = "language", required = false) String language) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters(id);
      addDataAccessAmendmentFormConfiguration(params, getDataAccessAmendment(params, amendmentId), !edit, language == null ? locale : language);

      List<String> permissions = Lists.newArrayList("VIEW", "EDIT", "DELETE").stream()
        .filter(action -> isAmendmentPermitted(action, id, amendmentId)).collect(Collectors.toList());
      if (isPermitted("/data-access-request/" + id + "/amendment/" + amendmentId, "EDIT", "_status"))
        permissions.add("EDIT_STATUS");

      params.put("amendmentPermissions", permissions);

      return new ModelAndView("data-access-amendment-form", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=data-access-amendment%2F" + id + "%3Fid%3D" + amendmentId);
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

  private DataAccessAmendment getDataAccessAmendment(Map<String, Object> params, String amendmentId) {
    DataAccessRequest dar = getDataAccessRequest(params);
    Optional<DataAccessAmendment> amendment = ((List<DataAccessAmendment>) params.get("amendments")).stream()
      .filter(a -> a.getId().equals(amendmentId)).findFirst();
    if (amendment.isPresent()) return amendment.get();
    throw new NoSuchElementException("Data access amendment " + amendmentId); // TODO is there specific exception?
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
    params.put("formConfig", new SchemaFormConfig(dataAccessForm.getSchema(), dataAccessForm.getDefinition(), request.getContent(), locale, readOnly));
    params.put("accessConfig", new DataAccessConfig(dataAccessForm));
  }

  private void addDataAccessAmendmentFormConfiguration(Map<String, Object> params, DataAccessAmendment amendment, boolean readOnly, String locale) {
    Optional<DataAccessForm> d = dataAccessFormService.find();
    if (!d.isPresent()) throw NoSuchDataAccessFormException.withDefaultMessage();
    Optional<DataAccessAmendmentForm> ad = dataAccessAmendmentFormService.find();
    if (!ad.isPresent()) throw NoSuchDataAccessFormException.withDefaultMessage();
    DataAccessForm dataAccessForm = d.get();
    DataAccessAmendmentForm dataAccessAmendmentForm = ad.get();
    params.put("amendment", amendment);
    params.put("formConfig", new SchemaFormConfig(dataAccessAmendmentForm.getSchema(), dataAccessAmendmentForm.getDefinition(), amendment.getContent(), locale, readOnly));
    params.put("accessConfig", new DataAccessConfig(dataAccessForm));
  }

  /**
   * Workflow settings.
   */
  public class DataAccessConfig {

    private final boolean withReview;
    private final boolean withConditionalApproval;
    private final boolean approvedFinal;
    private final boolean rejectedFinal;
    private final boolean amendmentsEnabled;

    private DataAccessConfig(DataAccessForm form) {
      this.withReview = form.isWithReview();
      this.withConditionalApproval = form.isWithConditionalApproval();
      this.approvedFinal = form.isApprovedFinal();
      this.rejectedFinal = form.isRejectedFinal();
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
  }

  /**
   * Schema form settings.
   */
  public class SchemaFormConfig {

    private final String schema;
    private final String definition;
    private final String model;
    private final boolean readOnly;

    private SchemaFormConfig(String schema, String definition, String model, String locale, boolean readOnly) {
      this.readOnly = readOnly;
      String lang = locale == null ? "en" : locale.replaceAll("\"", "");
      Translator translator = JsonTranslator.buildSafeTranslator(() -> micaConfigService.getTranslations(lang, false));
      translator = new PrefixedValueTranslator(translator);

      TranslationUtils translationUtils = new TranslationUtils();
      this.schema = translationUtils.translate(schema, translator).replaceAll("col-xs-", "col-");
      this.definition = translationUtils.translate(definition, translator).replaceAll("col-xs-", "col-");
      this.model = model;
    }

    public String getSchema() {
      return schema;
    }

    public String getDefinition() {
      return definition;
    }

    public String getModel() {
      return model;
    }

    public boolean isReadOnly() {
      return readOnly;
    }

  }
}
