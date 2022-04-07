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

import org.joda.time.DateTime;
import org.obiba.mica.access.DataAccessEntityRepository;
import org.obiba.mica.access.DataAccessFeasibilityRepository;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.DataAccessAmendment;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.DataAccessFeasibility;
import org.obiba.mica.access.event.DataAccessFeasibilityDeletedEvent;
import org.obiba.mica.access.event.DataAccessFeasibilityUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Validated
public class DataAccessFeasibilityService extends DataAccessEntityService<DataAccessFeasibility> {

  private static final Logger log = LoggerFactory.getLogger(DataAccessFeasibilityService.class);

  @Inject
  DataAccessRequestUtilService dataAccessRequestUtilService;

  @Inject
  private DataAccessFeasibilityRepository dataFeasibilityRequestRepository;

  @Override
  protected DataAccessEntityRepository<DataAccessFeasibility> getRepository() {
    return dataFeasibilityRequestRepository;
  }

  @Override
  public DataAccessFeasibility save(@NotNull DataAccessFeasibility feasibility) {
    DataAccessFeasibility saved = feasibility;
    DataAccessEntityStatus from = null;

    if (feasibility.isNew()) {
      setAndLogStatus(saved, DataAccessEntityStatus.OPENED);
      saved.setId(ensureUniqueId(saved.getParentId()));
    } else {
      saved = dataFeasibilityRequestRepository.findOne(feasibility.getId());
      if (saved != null) {
        from = saved.getStatus();
        // validate the status
        dataAccessRequestUtilService.checkStatusTransition(saved, feasibility.getStatus());
        saved.setStatus(feasibility.getStatus());
        if (feasibility.hasStatusChangeHistory()) saved.setStatusChangeHistory(feasibility.getStatusChangeHistory());
        // merge beans
        BeanUtils.copyProperties(feasibility, saved, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
          "lastModifiedDate", "statusChangeHistory");
      } else {
        saved = feasibility;
        setAndLogStatus(saved, DataAccessEntityStatus.OPENED);
      }
    }

    schemaFormContentFileService.save(saved, dataFeasibilityRequestRepository.findOne(feasibility.getId()),
      String.format("/data-access-request/%s/feasibility/%s", saved.getParentId(), feasibility.getId()));

    saved.setLastModifiedDate(DateTime.now());

    dataFeasibilityRequestRepository.save(saved);
    eventBus.post(new DataAccessFeasibilityUpdatedEvent(saved));
    sendNotificationEmails(saved, from);
    return saved;
  }

  private String ensureUniqueId(String parentId) {
    int count = findByParentId(parentId).size();
    String newId;

    do {
      count++;
      newId = String.format("%s-F%d", parentId, count);
    } while (null != dataFeasibilityRequestRepository.findOne(newId));

    return newId;
  }

  @Override
  protected Map<String, String> getNotificationEmailContext(DataAccessFeasibility request) {
    Map<String, String> notificationEmailContext = super.getNotificationEmailContext(request);
    notificationEmailContext.put("parentId", request.getParentId());
    return notificationEmailContext;
  }

  public List<DataAccessFeasibility> findByStatus(@Nullable String parentId, @Nullable List<String> status) {
    if (status == null || status.isEmpty()) return dataFeasibilityRequestRepository.findByParentId(parentId);

    List<DataAccessEntityStatus> statusList = status.stream().map(DataAccessEntityStatus::valueOf)
      .collect(Collectors.toList());

    return dataFeasibilityRequestRepository.findByParentId(parentId).stream()
      .filter(dar -> statusList.contains(dar.getStatus())).collect(Collectors.toList());
  }

  public List<DataAccessFeasibility> findByParentId(@NotNull String parentId) {
    return dataFeasibilityRequestRepository.findByParentId(parentId);
  }

  public int countByParentId(@NotNull String parentId) {
    return dataFeasibilityRequestRepository.countByParentId(parentId);
  }

  public int countPendingByParentId(@NotNull String parentId) {
    return dataFeasibilityRequestRepository.countPendingByParentId(parentId);
  }

  /**
   * Delete the {@link DataAccessFeasibility} matching the identifier.
   *
   * @param id
   * @throws NoSuchDataAccessRequestException
   */
  @Override
  public void delete(@NotNull String id) throws NoSuchDataAccessRequestException {
    delete(findById(id));
  }

  void changeApplicantAndSave(DataAccessFeasibility feasibility, String applicant) {
    feasibility.setApplicant(applicant);
    save(feasibility);
  }

  void delete(@NotNull DataAccessFeasibility feasibility) throws NoSuchDataAccessRequestException {
    schemaFormContentFileService.deleteFiles(feasibility);
    eventBus.post(new DataAccessFeasibilityDeletedEvent(feasibility));
    dataFeasibilityRequestRepository.delete(feasibility);
  }
}
