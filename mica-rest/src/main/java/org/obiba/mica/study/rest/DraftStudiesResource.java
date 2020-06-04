/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.rest;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.google.common.eventbus.EventBus;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.network.event.IndexNetworksEvent;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.event.IndexStudiesEvent;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;

import com.codahale.metrics.annotation.Timed;

@Path("/draft")
public class DraftStudiesResource {

  @Inject
  private StudyService studyService;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private Dtos dtos;

  @Inject
  private EventBus eventBus;

  @GET
  @Path("/studies")
  @Timed
  public List<Mica.StudyDto> list() {
    return studyService.findAllDraftStudies().stream()
      .filter(s -> subjectAclService.isPermitted("/draft/"+s.getResourcePath(), "VIEW", s.getId()))
      .sorted(Comparator.comparing(AbstractGitPersistable::getId))
      .map(s -> dtos.asDto(s, true))
      .collect(Collectors.toList());
  }


  @PUT
  @Path("/studies/_index")
  @Timed
  @RequiresPermissions({"/draft/individual-study:PUBLISH", "/draft/harmonization-study:PUBLISH"})
  public Response reIndex(@Nullable @QueryParam("id") List<String> ids) {
    eventBus.post(new IndexStudiesEvent(ids));
    return Response.noContent().build();
  }

}
