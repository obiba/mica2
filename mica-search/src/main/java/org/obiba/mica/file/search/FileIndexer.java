/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file.search;

import javax.inject.Inject;

import org.obiba.mica.file.FileUtils;
import org.obiba.mica.file.event.FileDeletedEvent;
import org.obiba.mica.file.event.FilePublishedEvent;
import org.obiba.mica.file.event.FileUnPublishedEvent;
import org.obiba.mica.file.event.FileUpdatedEvent;
import org.obiba.mica.search.ElasticSearchIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;

@Component
public class FileIndexer {

  private static final Logger log = LoggerFactory.getLogger(FileIndexer.class);

  public static final String ATTACHMENT_DRAFT_INDEX = "file-draft";

  public static final String ATTACHMENT_PUBLISHED_INDEX = "file-published";

  public static final String ATTACHMENT_TYPE = "AttachmentState";

  @Inject
  private ElasticSearchIndexer elasticSearchIndexer;

  @Async
  @Subscribe
  public void onFilePublished(FilePublishedEvent event) {
    log.debug("File {} was published", event.getPersistable());
    if (FileUtils.isDirectory(event.getPersistable())) return;
    elasticSearchIndexer.index(ATTACHMENT_DRAFT_INDEX, event.getPersistable());
    elasticSearchIndexer.index(ATTACHMENT_PUBLISHED_INDEX, event.getPersistable());
  }

  @Async
  @Subscribe
  public void onFileUnPublished(FileUnPublishedEvent event) {
    log.debug("File {} was unpublished", event.getPersistable());
    if (FileUtils.isDirectory(event.getPersistable())) return;
    elasticSearchIndexer.index(ATTACHMENT_DRAFT_INDEX, event.getPersistable());
    elasticSearchIndexer.delete(ATTACHMENT_PUBLISHED_INDEX, event.getPersistable());
  }

  @Async
  @Subscribe
  public void onFileDeleted(FileDeletedEvent event) {
    log.debug("File {} was deleted", event.getPersistable());
    if (FileUtils.isDirectory(event.getPersistable())) return;
    elasticSearchIndexer.delete(ATTACHMENT_DRAFT_INDEX, event.getPersistable());
    elasticSearchIndexer.delete(ATTACHMENT_PUBLISHED_INDEX, event.getPersistable());
  }

  @Async
  @Subscribe
  public void onFileUpdated(FileUpdatedEvent event) {
    log.debug("File {} was updated", event.getPersistable());
    if (FileUtils.isDirectory(event.getPersistable())) return;
    elasticSearchIndexer.index(ATTACHMENT_DRAFT_INDEX, event.getPersistable());
  }
}
