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
import org.obiba.mica.access.domain.DataAccessAmendment;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.domain.StatusChange;
import org.obiba.mica.micaConfig.domain.DataAccessAmendmentForm;
import org.obiba.mica.micaConfig.service.DataAccessAmendmentFormService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.web.controller.domain.DataAccessEntityDiff;
import org.obiba.mica.web.controller.domain.SchemaFormConfig;
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
public class DataAccessAmendmentFormController extends BaseDataAccessController {

  @Inject
  private DataAccessAmendmentFormService dataAccessAmendmentFormService;

  @GetMapping("/data-access-amendment-form/{id:.+}")
  public ModelAndView getAmendmentForm(@PathVariable String id,
                                       @RequestParam(value = "edit", defaultValue = "false") boolean edit,
                                       @CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale,
                                       @RequestParam(value = "language", required = false) String language) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newAmendmentParameters(id);
      String lg = getLang(locale, language);
      DataAccessAmendment amendment = getDataAccessAmendment(params);
      addDataAccessAmendmentFormConfiguration(params, amendment, !edit, lg);

      List<String> permissions = getPermissions(params);
      if (isPermitted("/data-access-request/private-comment", "VIEW", null))
        permissions.add("VIEW_PRIVATE_COMMENTS");

      params.put("permissions", permissions);

      // show differences with previous submission (if any)
      if (subject.hasRole(Roles.MICA_ADMIN) || subject.hasRole(Roles.MICA_DAO)) {
        List<StatusChange> submissions = amendment.getSubmissions();
        if (!DataAccessEntityStatus.OPENED.equals(amendment.getStatus())) {
          submissions = submissions.subList(0, submissions.size() - 1); // compare with previous submission, not with itself
        }
        String content = amendment.getContent();
        params.put("diffs", submissions.stream()
          .reduce((first, second) -> second)
          .map(change -> new DataAccessEntityDiff(change, dataAccessRequestUtilService.getContentDiff("data-access-form", change.getContent(), content, lg)))
          .filter(DataAccessEntityDiff::hasDifferences)
          .orElse(null));
      }

      return new ModelAndView("data-access-amendment-form", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=" + micaConfigService.getContextPath() + "/data-access-amendment-form%2F" + id);
    }
  }

  private Map<String, Object> newAmendmentParameters(String id) {
    DataAccessAmendment amendment = dataAccessAmendmentService.findById(id);
    Map<String, Object> params = newParameters(amendment.getParentId());
    params.put("amendment", amendment);

    DataAccessRequest dar = getDataAccessRequest(params);
    List<String> permissions = Lists.newArrayList("VIEW", "EDIT", "DELETE").stream()
      .filter(action -> ("VIEW".equals(action) || !dar.isArchived()) && isAmendmentPermitted(action, amendment.getParentId(), id)).collect(Collectors.toList());
    if (!dar.isArchived() && isPermitted("/data-access-request/" + amendment.getParentId() + "/amendment/" + id, "EDIT", "_status"))
      permissions.add("EDIT_STATUS");
    params.put("amendmentPermissions", permissions);

    return params;
  }

  private void addDataAccessAmendmentFormConfiguration(Map<String, Object> params, DataAccessAmendment amendment, boolean readOnly, String locale) {
    DataAccessAmendmentForm dataAccessAmendmentForm = getDataAccessAmendmentForm(amendment);
    params.put("formConfig", new SchemaFormConfig(micaConfigService, dataAccessAmendmentForm.getSchema(), dataAccessAmendmentForm.getDefinition(), amendment.getContent(), locale, readOnly));
    params.put("accessConfig", getConfig());
  }

  private boolean isAmendmentPermitted(String action, String id, String amendmentId) {
    return isPermitted("/data-access-request/" + id + "/amendment", action, amendmentId);
  }

  private DataAccessAmendment getDataAccessAmendment(Map<String, Object> params) {
    return (DataAccessAmendment) params.get("amendment");
  }

  private DataAccessAmendmentForm getDataAccessAmendmentForm(DataAccessAmendment amendment) {
    Optional<DataAccessAmendmentForm> form = dataAccessAmendmentFormService.findByRevision(amendment.hasFormRevision() ? amendment.getFormRevision().toString() : "latest");
    return form.orElseGet(() -> dataAccessAmendmentFormService.findByRevision("latest").get());
  }


}
