/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.service.StudyDatasetService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
@Path("/study-datasets")
@RequiresAuthentication
public class StudyDatasetsResource {

  @Inject
  private StudyDatasetService datasetService;

  @Inject
  private Dtos dtos;

  @GET
  public List<Mica.DatasetDto> get(@QueryParam("study") String studyId) {
    return datasetService.findAllStudyDatasets(studyId).stream().map(dtos::asDto).collect(Collectors.toList());
  }

}
