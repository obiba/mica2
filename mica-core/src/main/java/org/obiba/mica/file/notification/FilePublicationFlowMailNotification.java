/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file.notification;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.obiba.mica.core.domain.RevisionStatus;
import org.obiba.mica.core.notification.PublicationFlowMailNotification;
import org.obiba.mica.security.domain.SubjectAcl;
import org.springframework.stereotype.Component;

@Component
public class FilePublicationFlowMailNotification extends PublicationFlowMailNotification {

  public static final String FILE_NOTIFICATION_TEMPLATE = "fileStatusChanged";
  public static final String DEFAULT_FILE_NOTIFICATION_SUBJECT = "[${organization}] ${documentId}: file status has changed";

  public void send(String path, RevisionStatus current, RevisionStatus status) {
    if (micaConfigService.getConfig().isFsNotificationsEnabled() && current != status) {
      String[] documentParts = StringUtils.stripStart(path, "/").split("/");
      if(documentParts.length < 2) return;
      String documentInstance = String.format("/%s/%s", documentParts[0], documentParts[1]);

      Map<String, String> ctx = createContext();
      ctx.put("document", documentInstance);
      ctx.put("documentType", documentParts[0]);
      ctx.put("documentId", documentParts[1]);
      ctx.put("path", path);

      List<SubjectAcl> acls = subjectAclService.findByResourceInstance("/draft/file", documentInstance);
      String subject = mailService
        .getSubject(micaConfigService.getConfig().getFsNotificationsSubject(), ctx, DEFAULT_FILE_NOTIFICATION_SUBJECT);

      sendNotification(status, ctx, subject, FILE_NOTIFICATION_TEMPLATE, acls);
    }
  }
}
