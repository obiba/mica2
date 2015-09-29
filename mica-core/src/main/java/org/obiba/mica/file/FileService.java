package org.obiba.mica.file;

import java.io.IOException;
import java.io.InputStream;

/**
 * File storage service: read, write and delete operations.
 */
public interface FileService {

  /**
   * Get an {@link java.io.InputStream} from an existing stored file.
   *
   * @param id the file identifier
   * @return
   * @throws IOException
   */
  InputStream getFile(String id) throws IOException, GridFSFileNotFoundException;

  /**
   * Insert or update a file with the content of the {@link java.io.InputStream}.
   *
   * @param id
   * @param input
   */
  void save(String id, InputStream input);

  /**
   * Save a file from the {@link TempFileService} into the final file storage.
   *
   * @param tempFileId
   */
  void save(String tempFileId);

  /**
   * Delete the file (ignore if file does not exist).
   *
   * @param id
   */
  void delete(String id);

}
