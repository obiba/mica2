/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.search.rest.variable;

import javax.ws.rs.Path;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.dataset.search.DatasetIndexer;
import org.obiba.mica.study.search.StudyIndexer;

/**
 * Search for variables in the published study index.
 */
@Path("/variables/study/_search")
@RequiresAuthentication
public class PublishedStudyVariablesSearchResource extends AbstractVariablesSearchResource {

  @Override
  protected String getSearchIndex() {
    return StudyIndexer.PUBLISHED_STUDY_INDEX;
  }

  @Override
  protected String getSearchType() {
    return DatasetIndexer.VARIABLE_TYPE;
  }

}
