package org.obiba.mica.access;

import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.core.repository.AbstractAttachmentAwareRepository;
import org.obiba.mica.file.Attachment;

public class DataAccessRequestRepositoryImpl extends AbstractAttachmentAwareRepository<DataAccessRequest> implements DataAccessRequestRepositoryCustom {
  @Override
  protected String getAttachmentPath(DataAccessRequest obj, Attachment attachment) {
    return String.format("/dataAccessRequest/%s/attachment/%s", obj.getId(), attachment.getId());
  }
}
