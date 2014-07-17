/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.study;

import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.search.rest.PublishedDatasetsSearchResource;
import org.obiba.mica.web.model.Mica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
@Path("/study-datasets")
@RequiresAuthentication
public class PublishedStudyDatasetsResource extends PublishedDatasetsSearchResource<StudyDataset> {

  private static final Logger log = LoggerFactory.getLogger(PublishedStudyDatasetsResource.class);

  /**
   * Get {@link org.obiba.mica.dataset.domain.StudyDataset}s, optionally filtered by study.
   *
   * @param from
   * @param limit
   * @param sort
   * @param order
   * @param studyId
   * @return
   */
  @GET
  public List<Mica.DatasetDto> list(@QueryParam("from") @DefaultValue("0") int from,
      @QueryParam("limit") @DefaultValue("1000") int limit, @QueryParam("sort") String sort,
      @QueryParam("order") String order, @QueryParam("study") String studyId) {

    return getDatasetDtos(StudyDataset.class, from, limit, sort, order, studyId);
  }

  @Override
  protected String getStudyIdField() {
    return "studyTable.studyId";
  }
}
