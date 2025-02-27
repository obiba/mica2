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

import jakarta.ws.rs.Path;
import org.obiba.mica.core.service.PublishedDocumentService;
import org.obiba.mica.file.AttachmentState;
import org.obiba.mica.file.search.EsDraftFileService;
import org.obiba.mica.file.search.FileFilterHelper;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@Path("/draft/files-search")
@Scope("request")
@Component
public class DraftFilesSearchResource extends AbstractFileSearchResource {

  @Inject
  private EsDraftFileService esAttachmentService;

  @Override
  protected boolean isPublishedFileSystem() {
    return false;
  }

  @Override
  protected List<Mica.FileDto> searchFiles(int from, int limit, String sort, String order, String queryString) {
    PublishedDocumentService.Documents<AttachmentState> states = esAttachmentService
      .find(from, limit, sort, order, getBasePath(), queryString);

    return states.getList().stream().filter(this::isPermitted).map(state -> dtos.asFileDto(state, false, false))
      .collect(Collectors.toList());
  }

  private boolean isPermitted(AttachmentState state) {
    String path = state.getFullPath();
    // bypass check if access was already done in search filter
    return FileFilterHelper.appliesToFile(path) || subjectAclService.isPermitted("/draft/file", "VIEW", path);
  }
}
