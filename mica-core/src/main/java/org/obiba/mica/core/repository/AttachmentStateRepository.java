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

import java.util.List;

import org.obiba.mica.file.AttachmentState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface AttachmentStateRepository extends MongoRepository<AttachmentState, String>, DocumentRepository<AttachmentState> {

  /**
   * Find {@link AttachmentState}s matching the path regular expression.
   *
   * @param pathRegEx
   * @return
   */
  @Query("{'path': {$regex: ?0}}")
  List<AttachmentState> findByPath(String pathRegEx);

  @Query("{'path': {$regex: ?0}}")
  Page<AttachmentState> findByPath(String pathRegEx, Pageable pageable);

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
   * @param pathRegEx
   * @return
   */
  @Query(value = "{'path': {$regex: ?0}}", count = true)
  Long countByPath(String pathRegEx);

  /**
   * Count the number of published {@link AttachmentState}s matching the path.
   *
   * @param pathRegEx
   * @return
   */
  @Query(value = "{'path': {$regex: ?0}, 'publishedAttachment': { $exists: true }}", count = true)
  Long countByPathAndPublishedAttachmentNotNull(String pathRegEx);

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
   * @param pathRegEx
   * @param name
   * @return
   */
  @Query(value = "{'path': {$regex: ?0}, 'name': ?1}", count = true)
  Long countByPathAndName(String pathRegEx, String name);

  /**
   * Count the number of published {@link AttachmentState}s with given path and name (supposed to be a maximum of one).
   *
   * @param pathRegEx
   * @param name
   * @return
   */
  @Query(value = "{'path': {$regex: ?0}, 'name': ?1, 'publishedAttachment': { $exists: true }}", count = true)
  Long countByPathAndNameAndPublishedAttachmentNotNull(String pathRegEx, String name);
}
