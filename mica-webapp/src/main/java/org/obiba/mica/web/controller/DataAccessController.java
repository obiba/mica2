package org.obiba.mica.web.controller;

import com.google.common.collect.Maps;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import javax.ws.rs.QueryParam;
import java.util.Map;
import java.util.Optional;

@Controller
public class DataAccessController extends BaseController {

  @Inject
  private DataAccessRequestService dataAccessRequestService;

  @Inject
  DataAccessFormService dataAccessFormService;

  @Inject
  private MicaConfigService micaConfigService;

  @GetMapping("/data-access/{id}")
  public ModelAndView get(@PathVariable String id) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters();
      params.put("dar", getDataAccessRequest(id));
      params.put("pathPrefix", "../..");
      return new ModelAndView("data-access", params);
    } else {
      return new ModelAndView("redirect:signin?redirect=data-access/" + id);
    }
  }

  @GetMapping("/data-access-form/{id}")
  public ModelAndView getForm(@PathVariable String id, @CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale,
                              @RequestParam(value = "language", required = false) String language) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      DataAccessRequest dar = getDataAccessRequest(id);
      Map<String, Object> params = newParameters();
      params.put("dar", dar);
      params.put("pathPrefix", "../..");
      addDataAccessForm(params, dar.getContent(), language == null ? locale : language);
      return new ModelAndView("data-access-form", params);
    } else {
      return new ModelAndView("redirect:signin?redirect=data-access-form/" + id);
    }
  }

  private DataAccessRequest getDataAccessRequest(String id) {
    return dataAccessRequestService.findById(id);
  }

  private void addDataAccessForm(Map<String, Object> params, String model, String locale) {
    Optional<DataAccessForm> d = dataAccessFormService.find();
    if(!d.isPresent()) throw NoSuchDataAccessFormException.withDefaultMessage();
    params.put("form", new SchemaForm(d.get(), model, locale));
  }

  @GetMapping("/data-access/{id}/amendment/{aid}/form")
  public ModelAndView getAmendmentForm(@PathVariable String id, @PathVariable String aid) {
    return new ModelAndView("data-access-amendment-form");
  }

  @GetMapping("/data-access/{id}/comments")
  public ModelAndView getComments(@PathVariable String id) {
    return new ModelAndView("data-access-comments");
  }

  @GetMapping("/data-access/{id}/history")
  public ModelAndView getHistory(@PathVariable String id) {
    return new ModelAndView("data-access-history");
  }

  public class SchemaForm {

    private final String schema;
    private final String definition;
    private final String model;

    private SchemaForm(DataAccessForm form, String model, String locale) {
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
  }
}
