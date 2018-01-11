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

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.obiba.mica.core.domain.AbstractGitPersistable;
import org.obiba.mica.security.service.SubjectAclService;
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

}
