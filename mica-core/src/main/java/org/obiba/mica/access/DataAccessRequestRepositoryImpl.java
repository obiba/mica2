package org.obiba.mica.access;

import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.core.repository.AbstractAttachmentAwareRepository;

public class DataAccessRequestRepositoryImpl extends AbstractAttachmentAwareRepository<DataAccessRequest> implements DataAccessRequestRepositoryCustom {
  @Override
  protected String getAttachmentPath(DataAccessRequest dataAccessRequest) {
    return String.format("/data-access-request/%s/attachment", dataAccessRequest.getId());
  }
}
