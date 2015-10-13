/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.file.search.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Path;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.file.AttachmentState;
import org.obiba.mica.file.search.EsPublishedFileService;
import org.obiba.mica.core.service.PublishedDocumentService;
import org.obiba.mica.web.model.Mica;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/files-search")
@RequiresAuthentication
@Scope("request")
@Component
public class PublishedFilesSearchResource extends AbstractFileSearchResource {
  @Inject
  private EsPublishedFileService esAttachmentService;

  @Override
  protected List<Mica.FileDto> searchFiles(int from, int limit, String sort, String order, String queryString) {
    PublishedDocumentService.Documents<AttachmentState> states = esAttachmentService
      .find(from, limit, sort, order, null, queryString);

    return states.getList().stream().map(state -> dtos.asFileDto(state, true)).collect(Collectors.toList());
  }
}
