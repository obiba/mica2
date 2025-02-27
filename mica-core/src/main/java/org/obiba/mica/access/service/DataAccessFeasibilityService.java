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

import org.obiba.mica.access.DataAccessEntityRepository;
import org.obiba.mica.access.DataAccessFeasibilityRepository;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.DataAccessFeasibility;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.event.DataAccessFeasibilityDeletedEvent;
import org.obiba.mica.access.event.DataAccessFeasibilityUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Nullable;
import jakarta.inject.Inject;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
    boolean feasabilityIsNew = feasibility.isNew();

    if (feasabilityIsNew) {
      setAndLogStatus(saved, DataAccessEntityStatus.OPENED);
      saved.setId(ensureUniqueId(saved.getParentId()));
    } else {
      Optional<DataAccessFeasibility> found = dataFeasibilityRequestRepository.findById(feasibility.getId());
      if (found.isPresent()) {
        saved = found.get();
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

    schemaFormContentFileService.save(saved, dataFeasibilityRequestRepository.findById(feasibility.getId()),
      String.format("/data-access-request/%s/feasibility/%s", saved.getParentId(), feasibility.getId()));

    saved.setLastModifiedDate(LocalDateTime.now());

    if (feasabilityIsNew) dataFeasibilityRequestRepository.insert(saved);
    else dataFeasibilityRequestRepository.save(saved);
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
    } while (null != dataFeasibilityRequestRepository.findById(newId).orElse(null));

    return newId;
  }

  @Override
  protected String getMainRequestId(DataAccessFeasibility request) {
    return request.getParentId();
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

  void changeApplicantAndSave(DataAccessRequest request) {
    findByParentId(request.getId()).forEach(feasibility -> changeApplicantAndSave(feasibility, request.getApplicant()));
  }

  private void changeApplicantAndSave(DataAccessFeasibility feasibility, String applicant) {
    feasibility.setApplicant(applicant);
    save(feasibility);
  }

  void delete(@NotNull DataAccessFeasibility feasibility) throws NoSuchDataAccessRequestException {
    schemaFormContentFileService.deleteFiles(feasibility);
    if (feasibility.hasVariablesSet())
      variableSetService.delete(feasibility.getVariablesSet());
    eventBus.post(new DataAccessFeasibilityDeletedEvent(feasibility));
    dataFeasibilityRequestRepository.delete(feasibility);
  }
}
