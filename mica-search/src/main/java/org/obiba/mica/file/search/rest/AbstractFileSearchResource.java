package org.obiba.mica.file.search.rest;

import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.AttachmentState;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.Mica;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import static org.obiba.mica.file.FileUtils.isRoot;
import static org.obiba.mica.file.FileUtils.normalizePath;

public abstract class AbstractFileSearchResource {

  protected static final String DEFAULT_SORT = "name";

  protected static final String MAX_SIZE = "999";

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
    String pathPart = String.format("path:%s", QueryParser.escape(basePath));
    if(recursively) {
      pathPart = String.format("(%s OR %s\\/*)", pathPart, isRoot(basePath) ? "path:" : pathPart);
    }

    String queryString = Joiner.on(" AND ").join(
      Stream.of(pathPart, Strings.isNullOrEmpty(query) ? null : String.format("(%s)", query)).filter(s -> s != null)
        .iterator());

    return queryString;
  }

  @GET
  @Path("/{path:.*}")
  @Timed
  public List<Mica.FileDto> searchFiles(@PathParam("path") String path, @QueryParam("query") String query,
    @QueryParam("recursively") @DefaultValue("false") boolean recursively,
    @QueryParam("from") @DefaultValue("0") int from, @QueryParam("limit") @DefaultValue(MAX_SIZE) int limit,
    @QueryParam("sort") @DefaultValue(DEFAULT_SORT) String sort,
    @QueryParam("order") @DefaultValue("desc") String order) {
    basePath = normalizePath(path);
    if(!isPublishedFileSystem()) {
      subjectAclService.checkPermission("/draft/file", "VIEW", normalizePath(path));
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
