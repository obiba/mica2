/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
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
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.security.domain.SubjectAcl;
import org.springframework.stereotype.Component;

@Component
public class EntityPublicationFlowMailNotification extends PublicationFlowMailNotification {

  public static final String ENTITY_NOTIFICATION_TEMPLATE_FORMAT = "%sStatusChanged";

  public static final String DEFAULT_ENTITY_NOTIFICATION_SUBJECT_FORMAT
    = "[${organization}] ${documentId}: %s status has changed";

  public void send(String id, String typeName, RevisionStatus current, RevisionStatus status) {
    if(isEntityNotificationEnabled(current, status, typeName)) {
      Map<String, String> ctx = createContext();
      ctx.put("status", status.toString());
      ctx.put("documentType", typeName);
      ctx.put("documentId", id);

      // Get subjects having permissions on the specific instance or all instances of the considered type
      List<SubjectAcl> acls = getResourceAcls(String.format("/draft/%s", typeName), id);
      acls.addAll(getResourceAcls(String.format("/draft/%s", typeName), "*"));
      String subject = mailService
        .getSubject(micaConfigService.getConfig().getStudyNotificationsSubject(), ctx, getMailEntityTitle(typeName));

      sendNotification(status, ctx, subject, String.format(ENTITY_NOTIFICATION_TEMPLATE_FORMAT, typeName), acls);
    }
  }

  private String getMailEntityTitle(String typeName) {
    return String.format(DEFAULT_ENTITY_NOTIFICATION_SUBJECT_FORMAT, WordUtils.capitalize(typeName.replace("-", " ")));
  }

  private boolean isEntityNotificationEnabled(RevisionStatus current, RevisionStatus status, String typeName) {
    if(current.equals(status)) {
      return false;
    }

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

    throw new IllegalArgumentException("Invalid state " + typeName);
  }
}
