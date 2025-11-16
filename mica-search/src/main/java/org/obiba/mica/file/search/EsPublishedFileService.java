/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.obiba.mica.file.AttachmentState;
import org.obiba.mica.file.service.PublishedFileService;
import org.obiba.mica.search.AbstractIdentifiedDocumentService;
import org.obiba.mica.search.queries.AbstractDocumentQuery;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;

@Component
@Scope(scopeName = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class EsPublishedFileService extends AbstractIdentifiedDocumentService<AttachmentState> implements PublishedFileService {
  private static final Logger log = LoggerFactory.getLogger(AbstractDocumentQuery.class);

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private FileFilterHelper fileFilterHelper;

  private String basePath = "/";

  @Override
  public long getCount(String path) {
    return getCountByRql("like(path,%2F" + path.replaceAll("/", "%20") + "%2F*)");
  }

  @Override
  protected AttachmentState processHit(Searcher.DocumentResult res) throws IOException {
    return objectMapper.readValue(res.getSourceInputStream(), AttachmentState.class);
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
  protected Searcher.IdFilter getAccessibleIdFilter() {
    if (isOpenAccess()) return null;
    return fileFilterHelper.makePublishedPathFilter(basePath);
  }
}
