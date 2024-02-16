/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.harmonization;

import com.codahale.metrics.annotation.Timed;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.search.rest.AbstractPublishedDatasetsResource;
import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
@Path("/harmonized-datasets")
public class PublishedHarmonizedDatasetsResource extends AbstractPublishedDatasetsResource<HarmonizationDataset> {

  private static final Logger log = LoggerFactory.getLogger(PublishedHarmonizedDatasetsResource.class);

  /**
   * Get {@link org.obiba.mica.dataset.domain.HarmonizationDataset}s, optionally filtered by study.
   *
   * @param from
   * @param limit
   * @param sort
   * @param order
   * @param studyId
   * @return
   */
  @GET
  @Timed
  public Mica.DatasetsDto list(@QueryParam("from") @DefaultValue("0") int from,
                               @QueryParam("limit") @DefaultValue("10") int limit, @QueryParam("sort") String sort,
                               @QueryParam("order") String order, @QueryParam("study") String studyId, @QueryParam("query") String query) {

    return getDatasetDtos(HarmonizationDataset.class, from, limit, sort, order, studyId, query);
  }

  @Override
  protected String getStudyIdField() {
    return "studyTable.studyId";
  }
}
