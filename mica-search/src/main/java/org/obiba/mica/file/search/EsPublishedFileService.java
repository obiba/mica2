/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file.search;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.obiba.mica.file.AttachmentState;
import org.obiba.mica.file.service.PublishedFileService;
import org.obiba.mica.search.AbstractDocumentService;
import org.obiba.mica.search.queries.AbstractDocumentQuery;
import org.obiba.mica.spi.search.Indexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

@Component
@Scope(scopeName = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class EsPublishedFileService extends AbstractDocumentService<AttachmentState> implements PublishedFileService {
  private static final Logger log = LoggerFactory.getLogger(AbstractDocumentQuery.class);

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private FileFilterHelper fileFilterHelper;

  private String basePath = "/";

  @Override
  public long getCount(String path) {
    return getCount(QueryBuilders.wildcardQuery("path", String.format("/%s/*", path)));
  }

  @Override
  protected AttachmentState processHit(SearchHit hit) throws IOException {
    InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes());
    return objectMapper.readValue(inputStream, AttachmentState.class);
  }

  @Override
  protected String getIndexName() {
    return Indexer.ATTACHMENT_PUBLISHED_INDEX;
  }

  @Override
  protected String getType() {
    return Indexer.ATTACHMENT_TYPE;
  }

  @Override
  public Documents<AttachmentState> find(int from, int limit, @Nullable String sort, @Nullable String order,
    @Nullable String basePath, @Nullable String queryString) {
    this.basePath = basePath;
    List<String> fields = Lists.newArrayList("publishedAttachment.name.analyzed", "publishedAttachment.type.analyzed");
    fields.addAll(getLocalizedFields("publishedAttachment.description"));
    return find(from, limit, sort, order, null, queryString, fields);
  }

  @Nullable
  @Override
  protected QueryBuilder filterByAccess() {
    return fileFilterHelper.makePublishedFilesFilter(basePath);
  }

}
