package org.obiba.mica.web.controller;

import com.google.common.collect.Lists;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.obiba.core.translator.JsonTranslator;
import org.obiba.core.translator.PrefixedValueTranslator;
import org.obiba.core.translator.TranslationUtils;
import org.obiba.core.translator.Translator;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.micaConfig.NoSuchDataAccessFormException;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class DataAccessController extends BaseController {

  @Inject
  private DataAccessRequestService dataAccessRequestService;

  @Inject
  DataAccessFormService dataAccessFormService;

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private UserProfileService userProfileService;

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
      addDataAccessFormConfiguration(params, getDataAccessRequest(params).getContent(), !edit, language == null ? locale : language);
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
      return new ModelAndView("data-access-history", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=data-access-history%2F" + id);
    }
  }

  @GetMapping("/data-access-amendments/{id}")
  public ModelAndView getAmendments(@PathVariable String id) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters(id);
      return new ModelAndView("data-access-amendments", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=data-access-amendments%2F" + id);
    }
  }

  @GetMapping("/data-access-documents/{id}")
  public ModelAndView getDocuments(@PathVariable String id) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters(id);
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
      return new ModelAndView("data-access-comments", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=data-access-comments%2F" + id);
    }
  }

  //
  // Private methods
  //

  private Map<String, Object> newParameters(String id) {
    Map<String, Object> params = newParameters();
    DataAccessRequest dar = getDataAccessRequest(id);
    params.put("dar", dar);
    params.put("pathPrefix", "../..");

    params.put("applicant", userProfileService.asMap(userProfileService.getProfile(dar.getApplicant(), true)));

    List<String> permissions = Lists.newArrayList("VIEW", "EDIT", "DELETE").stream()
      .filter(action -> isPermitted(action, id)).collect(Collectors.toList());
    if (isPermitted("/data-access-request/" + id, "EDIT", "_status"))
      permissions.add("EDIT_STATUS");

    params.put("permissions", permissions);

    return params;
  }

  private boolean isPermitted(String action, String id) {
    return isPermitted("/data-access-request", action, id);
  }

  private DataAccessRequest getDataAccessRequest(Map<String, Object> params) {
    return (DataAccessRequest) params.get("dar");
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

  private void addDataAccessFormConfiguration(Map<String, Object> params, String model, boolean readOnly, String locale) {
    Optional<DataAccessForm> d = dataAccessFormService.find();
    if (!d.isPresent()) throw NoSuchDataAccessFormException.withDefaultMessage();
    DataAccessForm dataAccessForm = d.get();
    params.put("formConfig", new SchemaFormConfig(dataAccessForm, model, locale, readOnly));
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

    private DataAccessConfig(DataAccessForm form) {
      this.withReview = form.isWithReview();
      this.withConditionalApproval = form.isWithConditionalApproval();
      this.approvedFinal = form.isApprovedFinal();
      this.rejectedFinal = form.isRejectedFinal();
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
  }

  /**
   * Schema form settings.
   */
  public class SchemaFormConfig {

    private final String schema;
    private final String definition;
    private final String model;
    private final boolean readOnly;

    private SchemaFormConfig(DataAccessForm form, String model, String locale, boolean readOnly) {
      this.readOnly = readOnly;
      String lang = locale == null ? "en" : locale.replaceAll("\"", "");
      Translator translator = JsonTranslator.buildSafeTranslator(() -> micaConfigService.getTranslations(lang, false));
      translator = new PrefixedValueTranslator(translator);

      TranslationUtils translationUtils = new TranslationUtils();
      this.schema = translationUtils.translate(form.getSchema(), translator).replaceAll("col-xs-", "col-");
      this.definition = translationUtils.translate(form.getDefinition(), translator).replaceAll("col-xs-", "col-");
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
