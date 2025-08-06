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
import org.obiba.mica.access.domain.StatusChange;
import org.obiba.mica.micaConfig.domain.DataAccessPreliminaryForm;
import org.obiba.mica.micaConfig.service.DataAccessPreliminaryFormService;
import org.obiba.mica.security.Roles;
import org.obiba.mica.web.controller.domain.DataAccessEntityDiff;
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

@Controller
public class DataAccessPreliminaryFormController extends BaseDataAccessController {

  @Inject
  private DataAccessPreliminaryFormService dataAccessPreliminaryFormService;

  @GetMapping("/data-access-preliminary-form/{id:.+}")
  public ModelAndView getPreliminaryForm(@PathVariable String id,
                                         @RequestParam(value = "edit", defaultValue = "false") boolean edit,
                                         @CookieValue(value = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "${locale.validatedLocale:en}") String locale,
                                         @RequestParam(value = "language", required = false) String language) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      if (!getConfig().isPreliminaryEnabled()) {
        return new ModelAndView("redirect:/data-access-form/" + id);
      }
      Map<String, Object> params = newParameters(id);
      String lg = getLang(locale, language);
      DataAccessPreliminary preliminary = getDataAccessPreliminary(params);
      addDataAccessPreliminaryFormConfiguration(params, preliminary, !edit, lg);

      List<String> permissions = getPermissions(params);
      if (isPermitted("/data-access-request/private-comment", "VIEW", null))
        permissions.add("VIEW_PRIVATE_COMMENTS");

      params.put("permissions", permissions);

      // show differences with previous submission (if any)
      if (subject.hasRole(Roles.MICA_ADMIN) || subject.hasRole(Roles.MICA_DAO)) {
        List<StatusChange> submissions = preliminary.getSubmissions();
        if (!DataAccessEntityStatus.OPENED.equals(preliminary.getStatus())) {
          submissions = submissions.subList(0, submissions.size() - 1); // compare with previous submission, not with itself
        }
        String content = preliminary.getContent();
        params.put("diffs", submissions.stream()
          .reduce((first, second) -> second)
          .map(change -> new DataAccessEntityDiff(change, dataAccessRequestUtilService.getContentDiff("data-access-form", change.getContent(), content, lg)))
          .filter(DataAccessEntityDiff::hasDifferences)
          .orElse(null));
      }

      return new ModelAndView("data-access-preliminary-form", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=" + micaConfigService.getContextPath() + "/data-access-preliminary-form%2F" + id);
    }
  }

  private void addDataAccessPreliminaryFormConfiguration(Map<String, Object> params, DataAccessPreliminary preliminary, boolean readOnly, String locale) {
    DataAccessPreliminaryForm form = dataAccessPreliminaryFormService.findByRevision(preliminary.hasFormRevision() ? preliminary.getFormRevision().toString() : "latest").get();
    params.put("formConfig", new SchemaFormConfig(micaConfigService, form.getSchema(), form.getDefinition(), preliminary.getContent(), locale, readOnly));
    params.put("accessConfig", getConfig());
  }

}
