/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.search;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.obiba.mica.core.domain.DefaultEntityBase;
import org.obiba.mica.core.domain.EntityState;
import org.obiba.mica.search.AbstractDocumentService;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.service.PublishedStudyService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

public abstract class EsPublishedStudyService<K extends EntityState, V extends BaseStudy> extends AbstractDocumentService<V> implements PublishedStudyService<K, V> {

  @Override
  protected V processHit(SearchHit hit) throws IOException {
    InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes());
    return mapStreamToObject(inputStream);
  }

  @Override
  protected QueryBuilder filterByAccess() {
    if (micaConfigService.getConfig().isOpenAccess()) return null;
    List<String> ids = getStudyService().findPublishedStates().stream().map(DefaultEntityBase::getId)
      .filter(this::isAccessible)
      .collect(Collectors.toList());
    return ids.isEmpty()
      ? QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("id"))
      : QueryBuilders.idsQuery().ids(ids);
  }

  protected abstract boolean isAccessible(String studyId);

  protected abstract V mapStreamToObject(InputStream inputStream) throws IOException;
}
