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

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Path;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.core.service.PublishedDocumentService;
import org.obiba.mica.file.AttachmentState;
import org.obiba.mica.file.search.EsPublishedFileService;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

@Path("/files-search")
@RequiresAuthentication
@Scope("request")
@Component
public class PublishedFilesSearchResource extends AbstractFileSearchResource {

  @Inject
  private EsPublishedFileService esAttachmentService;

  @Override
  protected boolean isPublishedFileSystem() {
    return true;
  }

  @Override
  protected List<Mica.FileDto> searchFiles(int from, int limit, String sort, String order, String queryString) {
    List<Mica.FileDto> files = Lists.newArrayList();

    PublishedDocumentService.Documents<AttachmentState> states = esAttachmentService
      .find(from, limit, sort, order, getBasePath(), queryString);

    while (files.size()<limit && !states.getList().isEmpty()) {
      states.getList().stream().filter(this::isAccessible).map(state -> dtos.asFileDto(state, true, false)) //
          .forEach(f -> files.add(f));

      if(files.size() < limit) {
        from = from + limit;
        states = esAttachmentService.find(from, limit, sort, order, getBasePath(), queryString);
      }
    }

    return files;
  }

  private boolean isAccessible(AttachmentState state) {
    String path = state.getFullPath();
    return subjectAclService.isAccessible("/file", path);
  }
}
