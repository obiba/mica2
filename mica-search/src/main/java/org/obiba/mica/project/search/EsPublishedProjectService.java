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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import org.elasticsearch.search.SearchHit;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.project.service.ProjectService;
import org.obiba.mica.project.service.PublishedProjectService;
import org.obiba.mica.search.AbstractPublishedDocumentService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EsPublishedProjectService extends AbstractPublishedDocumentService<Project> implements
    PublishedProjectService {

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private ProjectService projectService;

  @Override
  protected Project processHit(SearchHit hit) throws IOException {
    InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes());
    return objectMapper.readValue(inputStream, Project.class);
  }

  @Override
  protected String getIndexName() {
    return ProjectIndexer.PUBLISHED_PROJECT_INDEX;
  }

  @Override
  protected String getType() {
    return ProjectIndexer.PROJECT_TYPE;
  }

  @Override
  public ProjectService getProjectService() {
    return projectService;
  }
}
