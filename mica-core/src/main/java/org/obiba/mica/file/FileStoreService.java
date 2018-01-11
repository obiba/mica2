/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file;

import java.io.InputStream;

/**
 * File storage service: read, write and delete operations.
 */
public interface FileStoreService {

  /**
   * Get an {@link java.io.InputStream} from an existing stored file.
   *
   * @param id the file identifier
   * @return
   * @throws FileRuntimeException
   */
  InputStream getFile(String id) throws FileRuntimeException;

  /**
   * Insert or update a file with the content of the {@link java.io.InputStream}.
   *
   * @param id
   * @param input
   * @throws FileRuntimeException
   */
  void save(String id, InputStream input) throws FileRuntimeException;

  /**
   * Save a file from the {@link org.obiba.mica.file.service.TempFileService} into the final file storage.
   *
   * @param tempFileId
   * @throws FileRuntimeException
   */
  void save(String tempFileId) throws FileRuntimeException;

  /**
   * Delete the file (ignore if file does not exist).
   *
   * @param id
   */
  void delete(String id);

}
