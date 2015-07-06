package org.obiba.mica.access;

import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.core.repository.AbstractAttachmentAwareRepository;
import org.obiba.mica.file.Attachment;

public class DataAccessRequestRepositoryImpl extends AbstractAttachmentAwareRepository<DataAccessRequest> implements DataAccessRequestRepositoryCustom {
  @Override
  protected String getAttachmentPath(DataAccessRequest dataAccessRequest, Attachment attachment) {
    return String.format("/data-access-request/%s/attachment/%s", dataAccessRequest.getId(), attachment.getId());
  }
}
