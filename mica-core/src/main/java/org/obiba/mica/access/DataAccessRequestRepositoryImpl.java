package org.obiba.mica.access;

import javax.inject.Inject;

import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.core.repository.AttachmentAwareRepository;
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.file.FileService;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class DataAccessRequestRepositoryImpl
  implements DataAccessRequestRepositoryCustom, AttachmentAwareRepository<DataAccessRequest> {

  @Inject
  AttachmentRepository attachmentRepository;

  @Inject
  FileService fileService;

  @Inject
  MongoTemplate mongoTemplate;

  @Override
  public AttachmentRepository getAttachmentRepository() {
    return attachmentRepository;
  }

  @Override
  public FileService getFileService() {
    return fileService;
  }

  @Override
  public String getAttachmentPath(DataAccessRequest dataAccessRequest) {
    return String.format("/data-access-request/%s", dataAccessRequest.getId());
  }

  @Override
  public DataAccessRequest saveWithReferences(DataAccessRequest dataAccessRequest) {
    saveAttachments(dataAccessRequest);
    mongoTemplate.save(dataAccessRequest);

    return dataAccessRequest;
  }

  @Override
  public void deleteWithReferences(DataAccessRequest dataAccessRequest) {
    mongoTemplate.remove(dataAccessRequest);
    deleteAttachments(dataAccessRequest);
  }
}
