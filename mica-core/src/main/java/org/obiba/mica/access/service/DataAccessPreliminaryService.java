/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access.service;

import org.apache.shiro.SecurityUtils;
import org.obiba.mica.access.DataAccessEntityRepository;
import org.obiba.mica.access.DataAccessPreliminaryRepository;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.DataAccessPreliminary;
import org.obiba.mica.access.event.DataAccessPreliminaryDeletedEvent;
import org.obiba.mica.access.event.DataAccessPreliminaryUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Validated
public class DataAccessPreliminaryService extends DataAccessEntityService<DataAccessPreliminary> {

  private static final Logger log = LoggerFactory.getLogger(DataAccessPreliminaryService.class);

  @Inject
  DataAccessRequestUtilService dataAccessRequestUtilService;

  @Inject
  private DataAccessPreliminaryRepository dataPreliminaryRequestRepository;

  @Override
  protected DataAccessEntityRepository<DataAccessPreliminary> getRepository() {
    return dataPreliminaryRequestRepository;
  }

  @Override
  public DataAccessPreliminary save(@NotNull DataAccessPreliminary preliminary) {
    DataAccessPreliminary saved = preliminary;
    DataAccessEntityStatus from = null;
    boolean isNew = preliminary.isNew();

    if (isNew) {
      setAndLogStatus(saved, DataAccessEntityStatus.OPENED);
      saved.setId(saved.getParentId());
    } else {
      Optional<DataAccessPreliminary> found = dataPreliminaryRequestRepository.findById(preliminary.getId());
      if (found.isPresent()) {
        saved = found.get();
        from = saved.getStatus();
        // validate the status
        dataAccessRequestUtilService.checkStatusTransition(saved, preliminary.getStatus());
        saved.setStatus(preliminary.getStatus());
        if (preliminary.hasStatusChangeHistory()) saved.setStatusChangeHistory(preliminary.getStatusChangeHistory());
        // merge beans
        BeanUtils.copyProperties(preliminary, saved, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
          "lastModifiedDate", "statusChangeHistory");
      } else {
        saved = preliminary;
        setAndLogStatus(saved, DataAccessEntityStatus.OPENED);
      }
    }

    schemaFormContentFileService.save(saved, dataPreliminaryRequestRepository.findById(preliminary.getId()),
      String.format("/data-access-request/%s/preliminary/%s", saved.getParentId(), preliminary.getId()));

    saved.setLastModifiedDate(LocalDateTime.now());

    if (isNew) dataPreliminaryRequestRepository.insert(saved);
    else dataPreliminaryRequestRepository.save(saved);
    eventBus.post(new DataAccessPreliminaryUpdatedEvent(saved));
    sendNotificationEmails(saved, from);
    return saved;
  }

  @Override
  protected String getMainRequestId(DataAccessPreliminary request) {
    return request.getParentId();
  }

  public List<DataAccessPreliminary> findByStatus(@Nullable String parentId, @Nullable List<String> status) {
    if (status == null || status.isEmpty()) return dataPreliminaryRequestRepository.findByParentId(parentId);

    List<DataAccessEntityStatus> statusList = status.stream().map(DataAccessEntityStatus::valueOf)
      .collect(Collectors.toList());

    return dataPreliminaryRequestRepository.findByParentId(parentId).stream()
      .filter(dar -> statusList.contains(dar.getStatus())).collect(Collectors.toList());
  }

  public List<DataAccessPreliminary> findByParentId(@NotNull String parentId) {
    return dataPreliminaryRequestRepository.findByParentId(parentId);
  }

  public int countByParentId(@NotNull String parentId) {
    return dataPreliminaryRequestRepository.countByParentId(parentId);
  }

  public int countPendingByParentId(@NotNull String parentId) {
    return dataPreliminaryRequestRepository.countPendingByParentId(parentId);
  }

  /**
   * Delete the {@link DataAccessPreliminary} matching the identifier.
   *
   * @param id
   * @throws NoSuchDataAccessRequestException
   */
  @Override
  public void delete(@NotNull String id) throws NoSuchDataAccessRequestException {
    delete(findById(id));
  }

  void changeApplicantAndSave(DataAccessPreliminary preliminary, String applicant) {
    preliminary.setApplicant(applicant);
    save(preliminary);
  }

  void delete(@NotNull DataAccessPreliminary preliminary) throws NoSuchDataAccessRequestException {
    schemaFormContentFileService.deleteFiles(preliminary);
    if (preliminary.hasVariablesSet())
      variableSetService.delete(preliminary.getVariablesSet());
    eventBus.post(new DataAccessPreliminaryDeletedEvent(preliminary));
    dataPreliminaryRequestRepository.delete(preliminary);
  }

  public DataAccessPreliminary getOrCreate(String id) {
    Optional<DataAccessPreliminary> preliminaryOpt = getRepository().findById(id);
    if (preliminaryOpt.isPresent()) return preliminaryOpt.get();
    else {
      DataAccessPreliminary preliminary = new DataAccessPreliminary();
      preliminary.setParentId(id);
      String applicant = SecurityUtils.getSubject().getPrincipal().toString();
      preliminary.setApplicant(applicant);
      return save(preliminary);
    }
  }
}
