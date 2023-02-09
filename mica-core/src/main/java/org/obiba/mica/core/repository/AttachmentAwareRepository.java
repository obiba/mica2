/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.repository;

import org.obiba.mica.core.domain.AttachmentAware;
import org.obiba.mica.file.FileStoreService;
import org.springframework.dao.DuplicateKeyException;

public interface AttachmentAwareRepository<T extends AttachmentAware> {
  AttachmentRepository getAttachmentRepository();

  FileStoreService getFileStoreService();

  String getAttachmentPath(T obj);

  default T saveAttachments(T obj) {
    obj.getAttachments().forEach(a -> {
      try {
        String defaultPath = getAttachmentPath(obj);
        if (!a.hasPath() || !a.getPath().startsWith(defaultPath)) a.setPath(defaultPath);
        this.getAttachmentRepository().save(a);
      } catch(DuplicateKeyException ex) {
        //ignore
      }
    });

    return obj;
  }

  default void deleteAttachments(T obj) {
    getAttachmentRepository().findByPath(String.format("^%s", getAttachmentPath(obj))).forEach(a -> {
      getAttachmentRepository().delete(a);
      getFileStoreService().delete(a.getFileReference());
    });
  }
}
