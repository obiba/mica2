/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest.variable;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.rest.harmonized.DataschemaDatasetVariableResource;
import org.obiba.mica.dataset.rest.study.StudyDatasetVariableResource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
@Path("/draft")
@RequiresAuthentication
public class DraftDatasetVariableResource {

  @Inject
  private ApplicationContext applicationContext;

  @Path("/variable/{id}")
  public DatasetVariableResource getVariable(@PathParam("id") String id) {
    DatasetVariableResource resource = null;
    DatasetVariable.IdResolver resolver = DatasetVariable.IdResolver.from(id);
    switch(resolver.getType()) {
      case Study:
        resource = applicationContext.getBean(StudyDatasetVariableResource.class);
        break;
      case Dataschema:
        resource = applicationContext.getBean(DataschemaDatasetVariableResource.class);
        break;
      case Harmonized:
        // TODO study specific
        //resource = applicationContext.getBean(HarmonizedDatasetVariableResource.class);
        break;
    }

    if (resource != null) {
      resource.setDatasetId(resolver.getDatasetId());
      resource.setName(resolver.getName());
      return resource;
    }

    throw new IllegalArgumentException("Not a valid variable identifier: " + id);
  }

}
