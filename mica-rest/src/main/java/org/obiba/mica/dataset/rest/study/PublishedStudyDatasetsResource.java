/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest.study;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.service.NoSuchDatasetException;
import org.obiba.mica.service.StudyDatasetService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
@Path("/")
@RequiresAuthentication
public class PublishedStudyDatasetsResource {

  @Inject
  private StudyDatasetService datasetService;

  @Inject
  private Dtos dtos;

  @Inject
  private ApplicationContext applicationContext;

  /**
   * Get all published {@link org.obiba.mica.domain.StudyDataset}, optionally filtered by study.
   *
   * @param studyId can be null, in which case all datasets are returned
   * @return
   */
  @GET
  @Path("/study-datasets")
  public List<Mica.DatasetDto> getDatasets(@QueryParam("study") String studyId) {
    return datasetService.findAllPublishedDatasets(studyId).stream().map(dtos::asDto).collect(Collectors.toList());
  }

  @Path("/study-dataset/{id}")
  public StudyDatasetResource getDataset(@PathParam("id") String id) {
    if (!datasetService.isPublished(id)) throw NoSuchDatasetException.withId(id);
    StudyDatasetResource resource = applicationContext.getBean(StudyDatasetResource.class);
    resource.setId(id);
    return resource;
  }

}
