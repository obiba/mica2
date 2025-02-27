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

import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.apache.commons.compress.utils.Lists;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.service.StudyService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.obiba.opal.web.model.Projects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

/**
 * REST controller for managing draft Study state.
 */
@Component
@Scope("request")
public class StudyStateResource {

  private static final Logger log = LoggerFactory.getLogger(StudyStateResource.class);

  @Inject
  private StudyService studyService;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private Dtos dtos;

  @Inject
  private OpalService opalService;

  private String id;
  private String resource;

  public void setId(String id) {
    this.id = id;
    this.resource = studyService.isCollectionStudy(id) ? "/draft/individual-study" : "/draft/harmonization-study";
  }

  @GET
  @Timed
  public Mica.StudySummaryDto get() {
    subjectAclService.checkPermission(resource, "VIEW", id);
    return dtos.asDto(studyService.getEntityState(id));
  }

  @GET
  @Path("/opal-projects")
  public List<Projects.ProjectDto> projects() throws URISyntaxException {
    subjectAclService.checkPermission(resource, "VIEW", id);
    String opalUrl = Optional.ofNullable(studyService.findStudy(id)).map(BaseStudy::getOpal).orElse(null);
    try {
      return opalService.getProjectDtos(opalUrl);
    } catch (Exception e) {
      log.warn("Failed at retrieving opal projects: {}", e.getMessage());
      return Lists.newArrayList();
    }
  }
}
