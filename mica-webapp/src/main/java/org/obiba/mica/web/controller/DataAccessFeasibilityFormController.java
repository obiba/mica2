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
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.DataAccessFeasibility;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.domain.StatusChange;
import org.obiba.mica.micaConfig.domain.DataAccessFeasibilityForm;
import org.obiba.mica.micaConfig.service.DataAccessFeasibilityFormService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.web.controller.domain.DataAccessEntityDiff;
import org.obiba.mica.micaConfig.service.helper.SchemaFormConfig;
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
public class DataAccessFeasibilityFormController extends BaseDataAccessController {

  @Inject
  private DataAccessFeasibilityFormService dataAccessFeasibilityFormService;

  @GetMapping("/data-access-feasibility-form/{id:.+}")
  public ModelAndView getFeasibilityForm(@PathVariable String id,
                                         @RequestParam(value = "edit", defaultValue = "false") boolean edit,
                                         @CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale,
                                         @RequestParam(value = "language", required = false) String language) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newFeasibilityParameters(id);
      String lg = getLang(locale, language);
      DataAccessFeasibility feasibility = getDataAccessFeasibility(params);
      addDataAccessFeasibilityFormConfiguration(params, feasibility, !edit, lg);

      List<String> permissions = getPermissions(params);
      if (isPermitted("/data-access-request/private-comment", "VIEW", null))
        permissions.add("VIEW_PRIVATE_COMMENTS");

      params.put("permissions", permissions);

      // show differences with previous submission (if any)
      if (subject.hasRole(Roles.MICA_ADMIN) || subject.hasRole(Roles.MICA_DAO)) {
        List<StatusChange> submissions = feasibility.getSubmissions();
        if (!DataAccessEntityStatus.OPENED.equals(feasibility.getStatus())) {
          submissions = submissions.subList(0, submissions.size() - 1); // compare with previous submission, not with itself
        }
        String content = feasibility.getContent();
        params.put("diffs", submissions.stream()
          .reduce((first, second) -> second)
          .map(change -> new DataAccessEntityDiff(change, dataAccessRequestUtilService.getContentDiff("data-access-form", change.getContent(), content, lg)))
          .filter(DataAccessEntityDiff::hasDifferences)
          .orElse(null));
      }

      return new ModelAndView("data-access-feasibility-form", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=" + micaConfigService.getContextPath() + "/data-access-feasibility-form%2F" + id);
    }
  }

  private Map<String, Object> newFeasibilityParameters(String id) {
    DataAccessFeasibility feasibility = dataAccessFeasibilityService.findById(id);
    Map<String, Object> params = newParameters(feasibility.getParentId());
    params.put("feasibility", feasibility);

    DataAccessRequest dar = getDataAccessRequest(params);
    List<String> permissions = Lists.newArrayList("VIEW", "EDIT", "DELETE").stream()
      .filter(action -> ("VIEW".equals(action) || !dar.isArchived()) && isFeasibilityPermitted(action, feasibility.getParentId(), id)).collect(Collectors.toList());
    if (!dar.isArchived() && isPermitted("/data-access-request/" + feasibility.getParentId() + "/feasibility/" + id, "EDIT", "_status"))
      permissions.add("EDIT_STATUS");
    params.put("feasibilityPermissions", permissions);

    return params;
  }

  private void addDataAccessFeasibilityFormConfiguration(Map<String, Object> params, DataAccessFeasibility feasibility, boolean readOnly, String locale) {
    DataAccessFeasibilityForm form = getDataAccessFeasibilityForm(feasibility);
    params.put("formConfig", new SchemaFormConfig(micaConfigService, form.getSchema(), form.getDefinition(), feasibility.getContent(), locale, readOnly));
    params.put("accessConfig", getConfig());
  }

  private boolean isFeasibilityPermitted(String action, String id, String feasibilityId) {
    return isPermitted("/data-access-request/" + id + "/feasibility", action, feasibilityId);
  }

  private DataAccessFeasibility getDataAccessFeasibility(Map<String, Object> params) {
    return (DataAccessFeasibility) params.get("feasibility");
  }

  private DataAccessFeasibilityForm getDataAccessFeasibilityForm(DataAccessFeasibility feasibility) {
    Optional<DataAccessFeasibilityForm> form = dataAccessFeasibilityFormService.findByRevision(feasibility.hasFormRevision() ? feasibility.getFormRevision().toString() : "latest");
    return form.orElseGet(() -> dataAccessFeasibilityFormService.findByRevision("latest").get());
  }
}
