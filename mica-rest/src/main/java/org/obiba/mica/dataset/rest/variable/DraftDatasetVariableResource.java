/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest.variable;

import javax.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.obiba.mica.dataset.DatasetVariableResource;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.rest.harmonization.DraftDataschemaDatasetVariableResource;
import org.obiba.mica.dataset.rest.harmonization.DraftHarmonizedDatasetVariableResource;
import org.obiba.mica.dataset.rest.collection.DraftCollectedDatasetVariableResource;
import org.obiba.mica.security.service.SubjectAclService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
@Path("/draft")
public class DraftDatasetVariableResource {

  @Inject
  private ApplicationContext applicationContext;

  @Inject
  private SubjectAclService subjectAclService;

  @Path("/variable/{id}")
  public DatasetVariableResource getVariable(@PathParam("id") String id) {
    DatasetVariableResource resource = null;
    DatasetVariable.IdResolver resolver = DatasetVariable.IdResolver.from(id);
    switch(resolver.getType()) {
      case Collected:
        subjectAclService.isPermitted("/draft/collected-dataset", "VIEW", resolver.getDatasetId());
        resource = applicationContext.getBean(DraftCollectedDatasetVariableResource.class);
        break;
      case Dataschema:
        subjectAclService.isPermitted("/draft/harmonized-dataset", "VIEW", resolver.getDatasetId());
        resource = applicationContext.getBean(DraftDataschemaDatasetVariableResource.class);
        break;
      case Harmonized:
        subjectAclService.isPermitted("/draft/harmonized-dataset", "VIEW", resolver.getDatasetId());
        resource = applicationContext.getBean(DraftHarmonizedDatasetVariableResource.class);
        ((DraftHarmonizedDatasetVariableResource) resource).setStudyId(resolver.getStudyId());
        ((DraftHarmonizedDatasetVariableResource) resource).setSource(resolver.getSource());
        break;
    }

    if(resource != null) {
      resource.setDatasetId(resolver.getDatasetId());
      resource.setVariableName(resolver.getName());
      return resource;
    }

    throw new IllegalArgumentException("Not a valid variable identifier: " + id);
  }

}
