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
import org.obiba.mica.file.service.DraftFileService;
import org.obiba.mica.search.AbstractIdentifiedDocumentService;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;

@Component
@Scope(scopeName = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class EsDraftFileService extends AbstractIdentifiedDocumentService<AttachmentState> implements DraftFileService {

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private FileFilterHelper fileFilterHelper;

  private String basePath = "/";

  @Override
  protected AttachmentState processHit(Searcher.DocumentResult res) throws IOException {
    return objectMapper.readValue(res.getSourceInputStream(), AttachmentState.class);
  }

  @Override
  public long getCount(String path) {
    return getCountByRql("like(path,%2F" + path.replaceAll("/", "%20") + "%2F*)");
  }

  @Override
  protected String getIndexName() {
    return Indexer.ATTACHMENT_DRAFT_INDEX;
  }

  @Override
  protected String getType() {
    return Indexer.ATTACHMENT_TYPE;
  }

  @Override
  public Documents<AttachmentState> find(int from, int limit, @Nullable String sort, @Nullable String order, @Nullable String basePath,
                                         @Nullable String queryString) {
    this.basePath = basePath;
    List<String> fields = Lists.newArrayList("attachment.name.analyzed", "attachment.type.analyzed");
    fields.addAll(getLocalizedFields("attachment.description"));
    return find(from, limit, sort, order, null, queryString, fields);
  }

  @Nullable
  @Override
  protected Searcher.IdFilter getAccessibleIdFilter() {
    return fileFilterHelper.makeDraftPathFilter(basePath);
  }
}
