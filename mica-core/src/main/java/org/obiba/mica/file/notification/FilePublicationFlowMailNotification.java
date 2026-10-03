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

  public static final String FILE_PUBLISHED_TEMPLATE = "filePublished";
  public static final String DEFAULT_FILE_PUBLISHED_SUBJECT = "[${organization}] ${documentId}: file has been ${published}";

  private static final String PUBLISHED_REQUIRED_ACTION = "EDIT";

  /**
   * Notify the status change of a folder and its files.
   */
  public void send(String path, RevisionStatus current, RevisionStatus status) {
    sendStatusChanged(path, path, current, status);
  }

  /**
   * Notify the status change of the file with the given name in the folder.
   */
  public void send(String folder, String name, RevisionStatus current, RevisionStatus status) {
    sendStatusChanged(folder, folder + "/" + name, current, status);
  }

  /**
   * Notify the publication (or unpublication) of a folder and its files.
   */
  public void sendPublished(String path, boolean published) {
    sendPublication(path, path, published);
  }

  /**
   * Notify the publication (or unpublication) of the file with the given name in the folder.
   */
  public void sendPublished(String folder, String name, boolean published) {
    sendPublication(folder, folder + "/" + name, published);
  }

  private void sendStatusChanged(String folder, String path, RevisionStatus current, RevisionStatus status) {
    if (micaConfigService.getConfig().isFsNotificationsEnabled() && current != status) {
      Map<String, String> ctx = createFileContext(folder, path);
      if (ctx == null) return;
      ctx.put("status", status.toString());

      sendNotification(status, ctx, getSubject(ctx, DEFAULT_FILE_NOTIFICATION_SUBJECT), FILE_NOTIFICATION_TEMPLATE,
        getFileAcls(ctx));
    }
  }

  private void sendPublication(String folder, String path, boolean published) {
    if (micaConfigService.getConfig().isFsNotificationsEnabled()) {
      Map<String, String> ctx = createFileContext(folder, path);
      if (ctx == null) return;
      ctx.put("published", published ? "published" : "unpublished");
      // same placeholder as the status change notification, so that both can share the subject
      ctx.put("status", published ? "PUBLISHED" : "UNPUBLISHED");

      sendNotification(PUBLISHED_REQUIRED_ACTION, ctx, getSubject(ctx, DEFAULT_FILE_PUBLISHED_SUBJECT),
        FILE_PUBLISHED_TEMPLATE, getFileAcls(ctx));
    }
  }

  /**
   * Context of the file (or folder) notification, null if the folder does not belong to a document.
   */
  private Map<String, String> createFileContext(String folder, String path) {
    // the document is resolved from the folder, as a file can be at the root of a document type
    String[] documentParts = StringUtils.stripStart(folder, "/").split("/");
    if(documentParts.length < 2) return null;

    Map<String, String> ctx = createContext();
    ctx.put("document", String.format("/%s/%s", documentParts[0], documentParts[1]));
    ctx.put("documentType", documentParts[0]);
    ctx.put("documentId", documentParts[1]);
    ctx.put("path", path);
    return ctx;
  }

  private List<SubjectAcl> getFileAcls(Map<String, String> ctx) {
    return subjectAclService.findByResourceInstance("/draft/file", ctx.get("document"));
  }

  private String getSubject(Map<String, String> ctx, String defaultSubject) {
    return mailService.getSubject(micaConfigService.getConfig().getFsNotificationsSubject(), ctx, defaultSubject);
  }
}
