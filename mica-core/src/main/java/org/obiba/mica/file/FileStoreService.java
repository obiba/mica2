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

import org.springframework.lang.Nullable;

import java.io.InputStream;
import java.util.Map;

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
   * Insert or update a file with the content of the {@link java.io.InputStream} along with some metadata.
   *
   * @param id
   * @param input
   * @param metadata - Key-Value entries
   * @throws FileRuntimeException
   */
  void saveWithMetaData(String id, InputStream input, @Nullable Map<String, String>  metadata) throws FileRuntimeException;

  /**
   * Save a file from the {@link org.obiba.mica.file.service.TempFileService} into the final file storage along with some metadata.
   *
   * @param tempFileId
   * @param metadata - Key-Value entries
   * @throws FileRuntimeException
   */
  void saveWithMetaData(String tempFileId, @Nullable Map<String, String> metadata) throws FileRuntimeException;

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

  /**
   * Delete the file (ignore if file does not exist).
   *
   * @param id
   */
  void deleteWithMetadata(String id, @Nullable Map<String, String> metadata);

}
