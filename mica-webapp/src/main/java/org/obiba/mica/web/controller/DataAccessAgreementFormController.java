/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.controller;

import com.google.common.collect.Lists;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.access.domain.DataAccessAgreement;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.micaConfig.domain.DataAccessAgreementForm;
import org.obiba.mica.micaConfig.service.DataAccessAgreementFormService;
import org.obiba.mica.micaConfig.service.helper.SchemaFormConfig;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class DataAccessAgreementFormController extends BaseDataAccessController {

  @Inject
  private DataAccessAgreementFormService dataAccessAgreementFormService;

  @GetMapping("/data-access-agreement-form/{id:.+}")
  public ModelAndView getAgreementForm(@PathVariable String id,
                                       @RequestParam(value = "edit", defaultValue = "false") boolean edit,
                                       @CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "${locale.validatedLocale:en}") String locale,
                                       @RequestParam(value = "language", required = false) String language) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newAgreementParameters(id);
      String lg = getLang(locale, language);
      DataAccessAgreement agreement = getDataAccessAgreement(params);
      addDataAccessAgreementFormConfiguration(params, agreement, !edit, lg);

      params.put("applicant", getUserProfileMap(agreement.getApplicant()));

      List<String> permissions = getPermissions(params);
      if (isPermitted("/data-access-request/private-comment", "VIEW", null))
        permissions.add("VIEW_PRIVATE_COMMENTS");

      params.put("permissions", permissions);

      return new ModelAndView("data-access-agreement-form", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=" + micaConfigService.getContextPath() + "/data-access-agreement-form%2F" + id);
    }
  }

  private Map<String, Object> newAgreementParameters(String id) {
    DataAccessAgreement agreement = dataAccessAgreementService.findById(id);
    Map<String, Object> params = newParameters(agreement.getParentId());
    params.put("agreement", agreement);

    DataAccessRequest dar = getDataAccessRequest(params);
    List<String> permissions = Lists.newArrayList("VIEW", "EDIT", "DELETE").stream()
      .filter(action -> ("VIEW".equals(action) || !dar.isArchived()) && isAgreementPermitted(action, agreement.getParentId(), id)).collect(Collectors.toList());
    if (!dar.isArchived() && isPermitted("/data-access-request/" + agreement.getParentId() + "/agreement/" + id, "EDIT", "_status"))
      permissions.add("EDIT_STATUS");
    params.put("agreementPermissions", permissions);

    return params;
  }

  private void addDataAccessAgreementFormConfiguration(Map<String, Object> params, DataAccessAgreement agreement, boolean readOnly, String locale) {
    DataAccessAgreementForm form = dataAccessAgreementFormService.findByRevision(agreement.hasFormRevision() ? agreement.getFormRevision().toString() : "latest").get();
    params.put("formConfig", new SchemaFormConfig(micaConfigService, form.getSchema(), form.getDefinition(), agreement.getContent(), locale, readOnly));
    params.put("accessConfig", getConfig());
  }

  private boolean isAgreementPermitted(String action, String id, String agreementId) {
    return isPermitted("/data-access-request/" + id + "/agreement", action, agreementId);
  }

  private DataAccessAgreement getDataAccessAgreement(Map<String, Object> params) {
    return (DataAccessAgreement) params.get("agreement");
  }

}
