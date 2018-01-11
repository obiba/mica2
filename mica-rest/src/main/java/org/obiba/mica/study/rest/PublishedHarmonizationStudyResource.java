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

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.obiba.mica.file.rest.FileResource;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;

/**
 * REST controller for managing Study.
 */
@Component
@Path("/harmonization-study/{id}")
@Scope("request")
@RequiresAuthentication
public class PublishedHarmonizationStudyResource extends AbstractPublishedStudyResource {

  @Inject
  PublishedDatasetService publishedDatasetService;

  @GET
  @Timed
  public Mica.StudyDto getStudyDto(@PathParam("id") String id, @QueryParam("locale") String locale,
    @QueryParam("participatingStudies") @DefaultValue("false") boolean participatingStudies) {
    checkAccess(id);
    List<HarmonizationDataset> datasets = participatingStudies
      ? publishedDatasetService.getHarmonizationDatasetsByStudy(id)
      : Lists.newArrayList();

    return dtos.asDto((HarmonizationStudy) getStudy(id, locale), false, datasets);
  }


  @Path("/file/{fileId}")
  public FileResource study(@PathParam("id") String id, @PathParam("fileId") String fileId) {
    return super.getStudyFileResource(id, fileId);
  }}
