/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
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
import org.elasticsearch.search.SearchHit;
import org.obiba.mica.file.AttachmentState;
import org.obiba.mica.search.AbstractPublishedDocumentService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

@Component
@Scope("request")
public class EsDraftFileService extends AbstractPublishedDocumentService<AttachmentState> {

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private FileFilterHelper fileFilterHelper;

  private String basePath = "/";

  @Override
  protected AttachmentState processHit(SearchHit hit) throws IOException {
    InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes());
    return objectMapper.readValue(inputStream, AttachmentState.class);
  }

  @Override
  protected String getIndexName() {
    return FileIndexer.ATTACHMENT_DRAFT_INDEX;
  }

  @Override
  protected String getType() {
    return FileIndexer.ATTACHMENT_TYPE;
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
  protected QueryBuilder filterByAccess() {
    return fileFilterHelper.makeDraftFilesFilter(basePath);
  }


}
