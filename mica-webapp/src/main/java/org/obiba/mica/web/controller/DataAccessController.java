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

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.obiba.mica.access.domain.DataAccessCollaborator;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.domain.DataAccessRequestTimeline;
import org.obiba.mica.access.notification.DataAccessRequestReportNotificationService;
import org.obiba.mica.access.service.DataAccessCollaboratorService;
import org.obiba.mica.core.domain.Comment;
import org.obiba.mica.security.Roles;
import org.obiba.mica.web.controller.domain.DataAccessCollaboratorBundle;
import org.obiba.mica.web.controller.domain.TimelineItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class DataAccessController extends BaseDataAccessController {

  @Value("${portal.files.extensions}")
  private String filesExtensions;

  @Inject
  private DataAccessCollaboratorService dataAccessCollaboratorService;

  @Inject
  private DataAccessRequestReportNotificationService dataAccessRequestReportNotificationService;

  @GetMapping("/data-access/{id:.+}")
  public ModelAndView get(@PathVariable String id, @RequestParam(value = "invitation", required = false) String invitation) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      if (!Strings.isNullOrEmpty(invitation) && getConfig().isCollaboratorsEnabled()) {
        // apply invitation if necessary, this will grant read access
        dataAccessCollaboratorService.acceptCollaborator(getDataAccessRequest(id), invitation);
      }
      Map<String, Object> params = newParameters(id);
      addDataAccessConfiguration(params);

      DataAccessRequestTimeline timeline = dataAccessRequestReportNotificationService.getReportsTimeline(getDataAccessRequest(params));
      params.put("reportTimeline", timeline);

      List<DataAccessCollaborator> collaborators = dataAccessCollaboratorService.findByRequestId(id);
      params.put("collaborators", collaborators.stream()
        .map(collaborator -> new DataAccessCollaboratorBundle(collaborator, getUserProfileMap(collaborator.hasPrincipal() ? collaborator.getPrincipal() : collaborator.getEmail())))
        .collect(Collectors.toList()));
      List<String> collaboratorEmails = collaborators.stream().map(DataAccessCollaborator::getEmail).collect(Collectors.toList());
      Set<String> suggestedCollaborators = Sets.newTreeSet();
      if (getConfig().isCollaboratorsEnabled()) {
        if (getDataAccessPreliminary(params) != null) {
          // suggest emails found in preliminary form
          suggestedCollaborators.addAll(dataAccessRequestUtilService.getEmails(getDataAccessPreliminary(params)).stream()
            .filter(email -> !collaboratorEmails.contains(email))
            .collect(Collectors.toSet()));
        }
        // suggest emails found in main form
        suggestedCollaborators.addAll(dataAccessRequestUtilService.getEmails(getDataAccessRequest(params)).stream()
          .filter(email -> !collaboratorEmails.contains(email))
          .collect(Collectors.toList()));
      }
      params.put("suggestedCollaborators", suggestedCollaborators);

      List<String> permissions = getPermissions(params);
      if (isArchivePermitted(getDataAccessRequest(params), timeline))
        permissions.add("ARCHIVE");
      else if (isUnArchivePermitted(getDataAccessRequest(params)))
        permissions.add("UNARCHIVE");

      if (isPermitted("/data-access-request/private-comment", "VIEW", null))
        permissions.add("VIEW_PRIVATE_COMMENTS");

      params.put("permissions", permissions);

      return new ModelAndView("data-access", params);
    } else {
      String path = micaConfigService.getContextPath() + "/data-access/" + id;
      if (!Strings.isNullOrEmpty(invitation) && getConfig().isCollaboratorsEnabled()) {
        path = path + "?invitation=" + invitation;
      }
      try {
        path = URLEncoder.encode(path, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        // ignore
      }
      return new ModelAndView("redirect:../signin?redirect=" + path);
    }
  }

  @GetMapping("/data-access-history/{id:.+}")
  public ModelAndView getHistory(@PathVariable String id) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters(id);
      addDataAccessConfiguration(params);

      List<String> permissions = getPermissions(params);
      if (isPermitted("/data-access-request/private-comment", "VIEW", null))
        permissions.add("VIEW_PRIVATE_COMMENTS");
      params.put("permissions", permissions);

      params.put("statusChangeEvents", getFormStatusChangeEvents(params));

      return new ModelAndView("data-access-history", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=" + micaConfigService.getContextPath() + "/data-access-history%2F" + id);
    }
  }

  @GetMapping("/data-access-documents/{id:.+}")
  public ModelAndView getDocuments(@PathVariable String id) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters(id);
      addDataAccessConfiguration(params);

      List<String> permissions = getPermissions(params);
      if (isPermitted("/data-access-request/private-comment", "VIEW", null))
        permissions.add("VIEW_PRIVATE_COMMENTS");

      params.put("permissions", permissions);

      params.put("filesExtensions", filesExtensions);

      return new ModelAndView("data-access-documents", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=" + micaConfigService.getContextPath() + "/data-access-documents%2F" + id);
    }
  }

  @GetMapping("/data-access-comments/{id:.+}")
  public ModelAndView getComments(@PathVariable String id) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters(id);
      addDataAccessConfiguration(params);

      List<String> permissions = getPermissions(params);

      if (isPermitted("/data-access-request/private-comment", "VIEW", null))
        permissions.add("VIEW_PRIVATE_COMMENTS");

      addTimelineItems(commentsService.findPublicComments("/data-access-request", id), params);

      params.put("permissions", permissions);

      return new ModelAndView("data-access-comments", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=" + micaConfigService.getContextPath() + "/data-access-comments%2F" + id);
    }
  }

  @GetMapping("/data-access-private-comments/{id:.+}")
  public ModelAndView getPrivateComments(@PathVariable String id) {
    Subject subject = SecurityUtils.getSubject();
    if (subject.isAuthenticated()) {
      Map<String, Object> params = newParameters(id);
      addDataAccessConfiguration(params);

      List<String> permissions = getPermissions(params);

      if (!isPermitted("/data-access-request/private-comment", "VIEW", null))
        checkPermission("/private-comment/data-access-request", "VIEW", null);
      else
        permissions.add("VIEW_PRIVATE_COMMENTS");

      params.put("permissions", permissions);

      addTimelineItems(commentsService.findPrivateComments("/data-access-request", id), params);

      return new ModelAndView("data-access-private-comments", params);
    } else {
      return new ModelAndView("redirect:../signin?redirect=" + micaConfigService.getContextPath() + "/data-access-private-comments%2F" + id);
    }
  }

  //
  // Private methods
  //

  private void addTimelineItems(List<Comment> comments, Map<String, Object> params) {
    params.put("comments", comments);
    List<TimelineItem> items = Lists.newArrayList();
    items.addAll(comments.stream().map(TimelineItem::new).collect(Collectors.toList()));
    items.addAll(getFormStatusChangeEvents(params).stream().map(TimelineItem::new).collect(Collectors.toList()));

    items = items.stream().sorted(Comparator.comparing(TimelineItem::getDate)).collect(Collectors.toList());
    // last comment may be removable
    DataAccessRequest dar = getDataAccessRequest(params);
    if (!dar.isArchived()) {

      TimelineItem item = items.stream().filter(TimelineItem::isCommentItem).reduce((first, second) -> second).orElse(null);
      if (item != null) {
        Subject subject = SecurityUtils.getSubject();
        item.setCanRemove(subject.getPrincipal().toString().equals(item.getAuthor()) || subject.hasRole(Roles.MICA_DAO) || subject.hasRole(Roles.MICA_ADMIN));
      }
    }
    params.put("items", items);
    params.put("authors", items.stream().map(TimelineItem::getAuthor).distinct()
      .collect(Collectors.toMap(u -> u, this::getUserProfileMap)));
  }

  private boolean isArchivePermitted(DataAccessRequest dar, DataAccessRequestTimeline timeline) {
    return !dar.isArchived() && (SecurityUtils.getSubject().hasRole(Roles.MICA_DAO) || SecurityUtils.getSubject().hasRole(Roles.MICA_ADMIN));
//    if (dar.isArchived()
//      || !DataAccessEntityStatus.APPROVED.equals(dar.getStatus())
//      || (!SecurityUtils.getSubject().hasRole(Roles.MICA_DAO) && !SecurityUtils.getSubject().hasRole(Roles.MICA_ADMIN))
//      || !timeline.hasEndDate()) return false;
//    return new Date().after(timeline.getEndDate());
  }

  private boolean isUnArchivePermitted(DataAccessRequest dar) {
    return dar.isArchived() && SecurityUtils.getSubject().hasRole(Roles.MICA_ADMIN);
  }

  private void addDataAccessConfiguration(Map<String, Object> params) {
    params.put("accessConfig", getConfig());
  }

}
