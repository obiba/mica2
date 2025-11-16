/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.project.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.project.service.ProjectService;
import org.obiba.mica.project.service.PublishedProjectService;
import org.obiba.mica.search.AbstractIdentifiedDocumentService;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

@Service
public class EsPublishedProjectService extends AbstractIdentifiedDocumentService<Project> implements
  PublishedProjectService {

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private ProjectService projectService;

  @Override
  protected Project processHit(Searcher.DocumentResult res) throws IOException {
    return objectMapper.readValue(res.getSourceInputStream(), Project.class);
  }

  @Override
  protected String getIndexName() {
    return Indexer.PUBLISHED_PROJECT_INDEX;
  }

  @Override
  protected String getType() {
    return Indexer.PROJECT_TYPE;
  }

  @Override
  public ProjectService getProjectService() {
    return projectService;
  }

  @Nullable
  @Override
  protected Searcher.IdFilter getAccessibleIdFilter() {
    if (isOpenAccess()) return null;
    return new Searcher.IdFilter() {
      @Override
      public Collection<String> getValues() {
        return projectService.findPublishedIds().stream()
          .filter(s -> subjectAclService.isAccessible("/project", s))
          .collect(Collectors.toList());
      }
    };
  }
}
