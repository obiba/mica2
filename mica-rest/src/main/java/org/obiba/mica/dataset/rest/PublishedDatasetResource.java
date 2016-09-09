/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest;

import javax.inject.Inject;
import javax.ws.rs.GET;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.dataset.NoSuchDatasetException;
import org.obiba.mica.dataset.domain.Dataset;
import org.obiba.mica.dataset.service.PublishedDatasetService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

/**
 * REST controller for managing Dataset.
 */
@Component
@Scope("request")
@RequiresAuthentication
public class PublishedDatasetResource {

  @Inject
  private PublishedDatasetService publishedDatasetService;

  @Inject
  private Dtos dtos;

  private String id;

  public void setId(String id) {
    this.id = id;
  }

  @GET
  @Timed
  public Mica.DatasetDto get() {
    Dataset dataset = publishedDatasetService.findById(id);
    if (dataset == null) throw NoSuchDatasetException.withId(id);
    return dtos.asDto(dataset);
  }

}
