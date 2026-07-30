/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.notification;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.WordUtils;
import org.obiba.mica.security.domain.SubjectAcl;
import org.springframework.stereotype.Component;

@Component
public class EntityPublishedMailNotification extends PublicationFlowMailNotification {

  public static final String ENTITY_NOTIFICATION_TEMPLATE_FORMAT = "%sPublished";

  public static final String DEFAULT_ENTITY_NOTIFICATION_SUBJECT_FORMAT
    = "[${organization}] ${documentId}: %s has been ${published}";

  private static final String REQUIRED_ACTION = "EDIT";

  public void send(String id, String typeName, boolean published) {
    if(isEntityNotificationEnabled(typeName)) {
      Map<String, String> ctx = createContext();
      ctx.put("published", published ? "published" : "unpublished");
      ctx.put("documentType", typeName);
      ctx.put("documentId", id);

      // Get subjects having permissions on the specific instance or all instances of the considered type
      List<SubjectAcl> acls = getResourceAcls(String.format("/draft/%s", typeName), id);
      acls.addAll(getResourceAcls(String.format("/draft/%s", typeName), "*"));
      String subject = mailService
        .getSubject(getNotificationsSubject(typeName), ctx, getMailEntityTitle(typeName));

      sendNotification(REQUIRED_ACTION, ctx, subject, String.format(ENTITY_NOTIFICATION_TEMPLATE_FORMAT, typeName), acls);
    }
  }

  private String getMailEntityTitle(String typeName) {
    return String.format(DEFAULT_ENTITY_NOTIFICATION_SUBJECT_FORMAT, WordUtils.capitalize(typeName.replace("-", " ")));
  }

  private String getNotificationsSubject(String typeName) {
    switch(typeName) {
      case "individual-study":
      case "harmonization-study":
        return micaConfigService.getConfig().getStudyNotificationsSubject();
      case "network":
        return micaConfigService.getConfig().getNetworkNotificationsSubject();
      case "collected-dataset":
        return micaConfigService.getConfig().getStudyDatasetNotificationsSubject();
      case "harmonized-dataset":
        return micaConfigService.getConfig().getHarmonizationDatasetNotificationsSubject();
      case "project":
        return micaConfigService.getConfig().getProjectNotificationsSubject();
    }

    throw new IllegalArgumentException("Invalid type " + typeName);
  }

  private boolean isEntityNotificationEnabled(String typeName) {
    switch(typeName) {
      case "individual-study":
      case "harmonization-study":
        return micaConfigService.getConfig().isStudyNotificationsEnabled();
      case "network":
        return micaConfigService.getConfig().isNetworkNotificationsEnabled();
      case "collected-dataset":
        return micaConfigService.getConfig().isStudyDatasetNotificationsEnabled();
      case "harmonized-dataset":
        return micaConfigService.getConfig().isHarmonizationDatasetNotificationsEnabled();
      case "project":
        return micaConfigService.getConfig().isProjectNotificationsEnabled();
    }

    throw new IllegalArgumentException("Invalid type " + typeName);
  }
}
