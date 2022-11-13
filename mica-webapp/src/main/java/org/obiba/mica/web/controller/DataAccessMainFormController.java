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

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.DataAccessPreliminary;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.domain.StatusChange;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.service.DataAccessFormService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.web.controller.domain.DataAccessConfigBundle;
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
import java.util.regex.Pattern;

@Controller
public class DataAccessMainFormController extends BaseDataAccessController {

  @Inject
  private DataAccessFormService dataAccessFormService;

  private final Pattern feasibilityIdPattern = Pattern.compile("-F\\d+$");

  private final Pattern amendmentIdPattern = Pattern.compile("-A\\d+$");

  @GetMapping("/data-access-form/{id:.+}")
  public ModelAndView getForm(@PathVariable String id,
                              @RequestParam(value = "edit", defaultValue = "false") boolean edit,
                              @CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "en") String locale,
                              @RequestParam(value = "language", required = false) String language) {
    if (amendmentIdPattern.matcher(id).find()) {
      return new ModelAndView("redirect:/data-access-amendment-form/" + id);
    } else if (feasibilityIdPattern.matcher(id).find()) {
      return new ModelAndView("redirect:/data-access-feasibility-form/" + id);
    }
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters(id);
      String lg = getLang(locale, language);
      DataAccessRequest dar = getDataAccessRequest(params);
      addDataAccessFormConfiguration(params, dar, !edit, lg);

      DataAccessPreliminary preliminary = getDataAccessPreliminary(params);
      if (DataAccessEntityStatus.OPENED.equals(dar.getStatus()) && preliminary != null && !DataAccessEntityStatus.APPROVED.equals(preliminary.getStatus())) {
        return new ModelAndView("redirect:/data-access-preliminary-form/" + id);
      }

      List<String> permissions = getPermissions(params);
      if (isPermitted("/data-access-request/private-comment", "VIEW", null))
        permissions.add("VIEW_PRIVATE_COMMENTS");

      params.put("permissions", permissions);

      // show differences with previous submission (if any)
      if (subject.hasRole(Roles.MICA_ADMIN) || subject.hasRole(Roles.MICA_DAO)) {
        List<StatusChange> submissions = dar.getSubmissions();
        if (!DataAccessEntityStatus.OPENED.equals(dar.getStatus()) && submissions.size() > 0) {
          submissions = submissions.subList(0, submissions.size() - 1); // compare with previous submission, not with itself
        }
        String content = dar.getContent();
        params.put("diffs", submissions.stream()
          .reduce((first, second) -> second)
          .map(change -> new DataAccessEntityDiff(change, dataAccessRequestUtilService.getContentDiff("data-access-form", change.getContent(), content, lg)))
          .filter(DataAccessEntityDiff::hasDifferences)
          .orElse(null));
      }

      return new ModelAndView("data-access-form", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=" + micaConfigService.getContextPath() + "/data-access-form%2F" + id);
    }
  }

  private void addDataAccessFormConfiguration(Map<String, Object> params, DataAccessRequest request, boolean readOnly, String locale) {
    DataAccessForm form = getDataAccessForm(request);
    params.put("formConfig", new SchemaFormConfig(micaConfigService, form.getSchema(), form.getDefinition(), request.getContent(), locale, readOnly));
    params.put("accessConfig", new DataAccessConfigBundle(getConfig(), form));
  }

  private DataAccessForm getDataAccessForm(DataAccessRequest request) {
    Optional<DataAccessForm> form = dataAccessFormService.findByRevision(request.hasFormRevision() ? request.getFormRevision().toString() : "latest");
    return form.orElseGet(() -> dataAccessFormService.findByRevision("latest").get());
  }
}
