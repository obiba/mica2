/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file.search.rest;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.AttachmentState;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.obiba.mica.file.FileUtils.isRoot;
import static org.obiba.mica.file.FileUtils.normalizePath;

public abstract class AbstractFileSearchResource {

  protected static final String DEFAULT_SORT = "name";

  protected static final String MAX_SIZE = "10000";

  @Inject
  protected SubjectAclService subjectAclService;

  @Inject
  protected Dtos dtos;

  private String basePath;

  /**
   * Specify if the file system view is published or draft. If published, only the published {@link AttachmentState}s
   * will be looked up and the corresponding {@link Attachment} will be returned. Otherwise all {@link AttachmentState}
   * and all revisions of {@link Attachment}s are accessible.
   *
   * @return
   */
  protected abstract boolean isPublishedFileSystem();

  protected abstract List<Mica.FileDto> searchFiles(int from, int limit, String sort, String order, String queryString);

  protected String getQueryString(String path, String query, boolean recursively) {
    String basePath = normalizePath(Strings.isNullOrEmpty(path) ? "/" : path);
    String pathPart = String.format("path:%s", escapeQuery(basePath));
    if (recursively) {
      pathPart = String.format("(%s OR %s\\/*)", pathPart, isRoot(basePath) ? "path:" : pathPart);
    }

    String queryString = Joiner.on(" AND ").join(
        Stream.of(pathPart, Strings.isNullOrEmpty(query) ? null : String.format("(%s)", query))
            .filter(Objects::nonNull)
            .iterator());

    return queryString;
  }

  private String escapeQuery(String query) {
    // TODO replace lucene QueryParser.escape()
    // escape spaces as well as forward slashes
    return query.replaceAll("\\/|\\\\\\/", "\\\\/").replaceAll("\\s+", "\\\\ ");
  }

  @GET
  @Path("/{path:.*}")
  @Timed
  public List<Mica.FileDto> searchFiles(@PathParam("path") String path, @QueryParam("query") String query,
                                        @QueryParam("recursively") @DefaultValue("false") boolean recursively,
                                        @QueryParam("from") @DefaultValue("0") int from, @QueryParam("limit") @DefaultValue(MAX_SIZE) int limit,
                                        @QueryParam("sort") @DefaultValue(DEFAULT_SORT) String sort,
                                        @QueryParam("order") @DefaultValue("desc") String order,
                                        @QueryParam("key") String shareKey) {

    basePath = normalizePath(path);
    if (!isPublishedFileSystem()) {
      subjectAclService.checkPermission("/draft/file", "VIEW", normalizePath(path), shareKey);
    }
    String queryString = getQueryString(path, query, recursively);

    return searchFiles(from, limit, sort, order, queryString);
  }

  /**
   * The path being searched.
   *
   * @return
   */
  public String getBasePath() {
    return basePath;
  }
}
