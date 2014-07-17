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

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.service.StudyDatasetService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
@Path("/")
@RequiresAuthentication
public class PublishedStudyDatasetResource {



  @Inject
  private StudyDatasetService datasetService;

  @Inject
  private ApplicationContext applicationContext;

  @Path("/study-dataset/{id}")
  public StudyDatasetResource getDataset(@PathParam("id") String id) {
    if (!datasetService.isPublished(id)) throw NoSuchDatasetException.withId(id);
    StudyDatasetResource resource = applicationContext.getBean(StudyDatasetResource.class);
    resource.setId(id);
    return resource;
  }

}
