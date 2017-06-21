/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.rest;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.service.CollectionStudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.web.model.Projects;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

/**
 * REST controller for managing draft Study state.
 */
@Component
@Scope("request")
public class StudyStateResource {

  @Inject
  private CollectionStudyService collectionStudyService;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private Dtos dtos;

  @Inject
  private OpalService opalService;

  private String id;

  public void setId(String id) {
    this.id = id;
  }

  @GET
  @Timed
  public Mica.StudySummaryDto get() {
    subjectAclService.checkPermission("/draft/collection-study", "VIEW", id);
    return dtos.asDto(collectionStudyService.getEntityState(id));
  }

  @GET
  @Path("/projects")
  public List<Projects.ProjectDto> projects() throws URISyntaxException {
    subjectAclService.checkPermission("/draft/collection-study", "VIEW", id);
    String opalUrl = Optional.ofNullable(collectionStudyService.findStudy(id)).map(Study::getOpal).orElse(null);

    return opalService.getProjectDtos(opalUrl);
  }
}
