/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.joda.time.DateTime;
import org.obiba.mica.access.DataAccessAmendmentRepository;
import org.obiba.mica.access.DataAccessEntityRepository;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.DataAccessAmendment;
import org.obiba.mica.access.domain.DataAccessEntity;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.StatusChange;
import org.obiba.mica.access.event.DataAccessAmendmentDeletedEvent;
import org.obiba.mica.access.event.DataAccessAmendmentUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class DataAccessAmendmentService extends DataAccessEntityService<DataAccessAmendment> {

  private static final Logger log = LoggerFactory.getLogger(DataAccessAmendmentService.class);

  @Inject
  private DataAccessAmendmentRepository dataAmendmentRequestRepository;

  @Override
  protected DataAccessEntityRepository<DataAccessAmendment> getRepository() {
    return dataAmendmentRequestRepository;
  }

  @Override
  public DataAccessAmendment save(@NotNull DataAccessAmendment amendment) {
    DataAccessAmendment saved = amendment;
    DataAccessEntityStatus from = null;

    if(amendment.isNew()) {
      setAndLogStatus(saved, DataAccessEntityStatus.OPENED);
      saved.setId(generateId());
    } else {
      saved = dataAmendmentRequestRepository.findOne(amendment.getId());
      if(saved != null) {
        from = saved.getStatus();
        // validate the status
        dataAccessRequestUtilService.checkStatusTransition(saved, amendment.getStatus());
        saved.setStatus(amendment.getStatus());
        if(amendment.hasStatusChangeHistory()) saved.setStatusChangeHistory(amendment.getStatusChangeHistory());
        // merge beans
        BeanUtils.copyProperties(amendment, saved, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
          "lastModifiedDate", "statusChangeHistory");
      } else {
        saved = amendment;
        setAndLogStatus(saved, DataAccessEntityStatus.OPENED);
      }
    }

    schemaFormContentFileService.save(saved, dataAmendmentRequestRepository.findOne(amendment.getId()),
      String.format("/data-access-request/%s/amendment/%s", saved.getParentId(), amendment.getId()));

    saved.setLastModifiedDate(DateTime.now());

    dataAmendmentRequestRepository.save(saved);
    eventBus.post(new DataAccessAmendmentUpdatedEvent(saved));
    sendNotificationEmails(saved, from);
    return saved;
  }

  @Override
  protected Map<String, String> getNotificationEmailContext(DataAccessAmendment request) {
    Map<String, String> notificationEmailContext = super.getNotificationEmailContext(request);
    notificationEmailContext.put("parentId", request.getParentId());
    return notificationEmailContext;
  }

  public List<DataAccessAmendment> findByStatus(@Nullable String parentId, @Nullable List<String> status) {
    if(status == null || status.isEmpty()) return dataAmendmentRequestRepository.findByParentId(parentId);

    List<DataAccessEntityStatus> statusList = status.stream().map(DataAccessEntityStatus::valueOf)
      .collect(Collectors.toList());

    return dataAmendmentRequestRepository.findByParentId(parentId).stream()
      .filter(dar -> statusList.contains(dar.getStatus())).collect(Collectors.toList());
  }

  public List<DataAccessAmendment> findByParentId(@NotNull String parentId) {
    return dataAmendmentRequestRepository.findByParentId(parentId);
  }

  public Map<String, List<StatusChange>> getCongregatedAmendmentStatusChangesFor(@NotNull String dataAccessRequestId) {
    return dataAmendmentRequestRepository.findByParentId(dataAccessRequestId).stream()
      .collect(Collectors.toMap(DataAccessAmendment::getId, DataAccessAmendment::getStatusChangeHistory));
  }

  /**
   * Delete the {@link DataAccessAmendment} matching the identifier.
   *
   * @param id
   * @throws NoSuchDataAccessRequestException
   */
  @Override
  public void delete(@NotNull String id) throws NoSuchDataAccessRequestException {
    delete(findById(id));
  }

  void delete(@NotNull DataAccessAmendment amendment) throws NoSuchDataAccessRequestException {
    schemaFormContentFileService.deleteFiles(amendment);
    eventBus.post(new DataAccessAmendmentDeletedEvent(amendment));
    dataAmendmentRequestRepository.delete(amendment);
  }
}
