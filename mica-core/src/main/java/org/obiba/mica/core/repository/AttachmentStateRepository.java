package org.obiba.mica.core.repository;

import java.util.List;

import org.obiba.mica.file.AttachmentState;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface AttachmentStateRepository extends MongoRepository<AttachmentState, String> {

  /**
   * Find {@link AttachmentState}s matching the path regular expression.
   *
   * @param pathRegEx
   * @return
   */
  @Query("{'path': {$regex: ?0}}")
  List<AttachmentState> findByPath(String pathRegEx);

  /**
   * Find published {@link AttachmentState}s matching the path regular expression.
   *
   * @param pathRegEx
   * @return
   */
  @Query("{'path': {$regex: ?0}, 'publishedAttachment': { $exists: true }}")
  List<AttachmentState> findByPathAndPublishedAttachmentNotNull(String pathRegEx);

  /**
   * Count the number of {@link AttachmentState}s matching the path.
   *
   * @param path
   * @return
   */
  Long countByPath(String path);

  /**
   * Count the number of published {@link AttachmentState}s matching the path.
   *
   * @param path
   * @return
   */
  Long countByPathAndPublishedAttachmentNotNull(String path);

  /**
   * Get the {@link AttachmentState}s with given path and name (supposed to be a maximum of one).
   *
   * @param path
   * @param name
   * @return
   */
  List<AttachmentState> findByPathAndName(String path, String name);

  /**
   * Get the published {@link AttachmentState}s with given path and name (supposed to be a maximum of one).
   *
   * @param path
   * @param name
   * @return
   */
  List<AttachmentState> findByPathAndNameAndPublishedAttachmentNotNull(String path, String name);

  /**
   * Count the number of {@link AttachmentState}s with given path and name (supposed to be a maximum of one).
   *
   * @param path
   * @param name
   * @return
   */
  Long countByPathAndName(String path, String name);

  /**
   * Count the number of published {@link AttachmentState}s with given path and name (supposed to be a maximum of one).
   *
   * @param path
   * @param name
   * @return
   */
  Long countByPathAndNameAndPublishedAttachmentNotNull(String path, String name);
}
