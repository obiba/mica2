/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file.search;

import javax.inject.Inject;

import org.obiba.mica.core.repository.AttachmentStateRepository;
import org.obiba.mica.file.AttachmentState;
import org.obiba.mica.file.FileUtils;
import org.obiba.mica.file.event.FileDeletedEvent;
import org.obiba.mica.file.event.FilePublishedEvent;
import org.obiba.mica.file.event.FileUnPublishedEvent;
import org.obiba.mica.file.event.FileUpdatedEvent;
import org.obiba.mica.file.event.IndexFilesEvent;
import org.obiba.mica.spi.search.Indexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;

@Component
public class FileIndexer {

  private static final Logger log = LoggerFactory.getLogger(FileIndexer.class);

  @Inject
  private Indexer indexer;

  @Inject
  private AttachmentStateRepository attachmentStateRepository;

  @Async
  @Subscribe
  public void onFilePublished(FilePublishedEvent event) {
    log.debug("File {} was published", event.getPersistable());
    if (FileUtils.isDirectory(event.getPersistable())) return;
    indexer.index(Indexer.ATTACHMENT_DRAFT_INDEX, event.getPersistable());
    indexer.index(Indexer.ATTACHMENT_PUBLISHED_INDEX, event.getPersistable());
  }

  @Async
  @Subscribe
  public void onFileUnPublished(FileUnPublishedEvent event) {
    log.debug("File {} was unpublished", event.getPersistable());
    if (FileUtils.isDirectory(event.getPersistable())) return;
    indexer.index(Indexer.ATTACHMENT_DRAFT_INDEX, event.getPersistable());
    indexer.delete(Indexer.ATTACHMENT_PUBLISHED_INDEX, event.getPersistable());
  }

  @Async
  @Subscribe
  public void onFileDeleted(FileDeletedEvent event) {
    log.debug("File {} was deleted", event.getPersistable());
    if (FileUtils.isDirectory(event.getPersistable())) return;
    indexer.delete(Indexer.ATTACHMENT_DRAFT_INDEX, event.getPersistable());
    indexer.delete(Indexer.ATTACHMENT_PUBLISHED_INDEX, event.getPersistable());
  }

  @Async
  @Subscribe
  public void onFileUpdated(FileUpdatedEvent event) {
    log.debug("File {} was updated", event.getPersistable());
    if (FileUtils.isDirectory(event.getPersistable())) return;
    indexer.index(Indexer.ATTACHMENT_DRAFT_INDEX, event.getPersistable());
  }

  @Async
  @Subscribe
  public void reIndexAll(IndexFilesEvent event) {
    if(indexer.hasIndex(Indexer.ATTACHMENT_DRAFT_INDEX)) indexer.dropIndex(Indexer.ATTACHMENT_DRAFT_INDEX);
    if(indexer.hasIndex(Indexer.ATTACHMENT_PUBLISHED_INDEX)) indexer.dropIndex(Indexer.ATTACHMENT_PUBLISHED_INDEX);

    Pageable pageRequest = new PageRequest(0, 100);
    Page<AttachmentState> attachments;

    do {
      attachments = attachmentStateRepository.findAll(pageRequest);
      attachments.forEach(a -> {
        if (FileUtils.isDirectory(a)) return;

        indexer.index(Indexer.ATTACHMENT_DRAFT_INDEX, a);

        if(a.getPublishedAttachment() != null) {
          indexer.index(Indexer.ATTACHMENT_PUBLISHED_INDEX, a);
        }
      });
    } while((pageRequest = attachments.nextPageable()) != null);
  }
}
