/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.project.search;

import javax.inject.Inject;

import org.obiba.mica.project.domain.Project;
import org.obiba.mica.project.event.IndexProjectsEvent;
import org.obiba.mica.project.event.ProjectDeletedEvent;
import org.obiba.mica.project.event.ProjectPublishedEvent;
import org.obiba.mica.project.event.ProjectUnpublishedEvent;
import org.obiba.mica.project.event.ProjectUpdatedEvent;
import org.obiba.mica.project.service.ProjectService;
import org.obiba.mica.search.ElasticSearchIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;

@Component
public class ProjectIndexer {

  private static final Logger log = LoggerFactory.getLogger(ProjectIndexer.class);

  public static final String DRAFT_PROJECT_INDEX = "project-draft";

  public static final String PUBLISHED_PROJECT_INDEX = "project-published";

  public static final String PROJECT_TYPE = "Project";

  public static final String[] LOCALIZED_ANALYZED_FIELDS = {"title", "summary"};

  @Inject
  private ProjectService projectService;

  @Inject
  private ElasticSearchIndexer elasticSearchIndexer;

  @Async
  @Subscribe
  public void networkUpdated(ProjectUpdatedEvent event) {
    log.info("Project {} was updated", event.getPersistable());
    elasticSearchIndexer.index(DRAFT_PROJECT_INDEX, event.getPersistable());
  }

  @Async
  @Subscribe
  public void networkPublished(ProjectPublishedEvent event) {
    log.info("Project {} was published", event.getPersistable());
    elasticSearchIndexer.index(PUBLISHED_PROJECT_INDEX, event.getPersistable());
  }

  @Async
  @Subscribe
  public void networkPublished(ProjectUnpublishedEvent event) {
    log.info("Project {} was unpublished", event.getPersistable());
    elasticSearchIndexer.delete(PUBLISHED_PROJECT_INDEX, event.getPersistable());
  }

  @Async
  @Subscribe
  public void networkDeleted(ProjectDeletedEvent event) {
    log.info("Project {} was deleted", event.getPersistable());
    elasticSearchIndexer.delete(DRAFT_PROJECT_INDEX, event.getPersistable());
    elasticSearchIndexer.delete(PUBLISHED_PROJECT_INDEX, event.getPersistable());
  }

  @Async
  @Subscribe
  public void reIndexProjects(IndexProjectsEvent event) {
    log.info("Reindexing all networks");
    reIndexAll(PUBLISHED_PROJECT_INDEX, projectService.findAllPublishedProjects());
    reIndexAll(DRAFT_PROJECT_INDEX, projectService.findAllProjects());
  }

  private void reIndexAll(String indexName, Iterable<Project> networks) {
    if(elasticSearchIndexer.hasIndex(indexName)) elasticSearchIndexer.dropIndex(indexName);
    elasticSearchIndexer.indexAll(indexName, networks);
  }
}
