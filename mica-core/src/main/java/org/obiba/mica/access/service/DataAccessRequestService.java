package org.obiba.mica.access.service;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.access.DataAccessRequestRepository;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.network.NoSuchNetworkException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Strings;

@Service
@Validated
public class DataAccessRequestService {

  @Inject
  private DataAccessRequestRepository dataAccessRequestRepository;

  public void save(@NotNull DataAccessRequest request) {
    DataAccessRequest saved = request;
    if(request.isNew()) {
      saved.setStatus(DataAccessRequest.Status.OPENED);
      //generateId(saved);
    } else {
      saved = dataAccessRequestRepository.findOne(request.getId());
      if(saved != null) {
        checkStatusTransition(saved.getStatus(), request.getStatus());
        BeanUtils.copyProperties(request, saved, "id", "version", "createdBy", "createdDate", "lastModifiedBy",
          "lastModifiedDate");
      } else {
        saved = request;
        saved.setStatus(DataAccessRequest.Status.OPENED);
      }
    }

    dataAccessRequestRepository.save(saved);
  }

  /**
   * Delete the {@link org.obiba.mica.access.domain.DataAccessRequest} matching the identifier.
   *
   * @param id
   * @throws NoSuchDataAccessRequestException
   */
  public void delete(@NotNull String id) throws NoSuchDataAccessRequestException {
    findById(id);
    dataAccessRequestRepository.delete(id);
  }

  /**
   * Update the status of the {@link org.obiba.mica.access.domain.DataAccessRequest} matching the identifier.
   *
   * @param id
   * @param status
   * @throws NoSuchNetworkException
   */
  public DataAccessRequest updateStatus(@NotNull String id, @NotNull DataAccessRequest.Status status)
    throws NoSuchDataAccessRequestException {
    DataAccessRequest request = findById(id);
    checkStatusTransition(request.getStatus(), status);
    request.setStatus(status);
    save(request);
    return request;
  }

  /**
   * Update the content of the {@link org.obiba.mica.access.domain.DataAccessRequest} matching the identifier.
   *
   * @param id
   * @param content
   */
  public void updateContent(@NotNull String id, String content) {
    DataAccessRequest request = findById(id);
    if(request.getStatus() != DataAccessRequest.Status.OPENED)
      throw new IllegalArgumentException("Data access request content can only be modified when status is draft");
    request.setContent(content);
    save(request);
  }

  //
  // Finders
  //

  /**
   * Get the {@link org.obiba.mica.access.domain.DataAccessRequest} matching the identifier.
   *
   * @param id
   * @return
   * @throws NoSuchNetworkException
   */
  @NotNull
  public DataAccessRequest findById(@NotNull String id) throws NoSuchNetworkException {
    DataAccessRequest request = dataAccessRequestRepository.findOne(id);
    if(request == null) throw NoSuchDataAccessRequestException.withId(id);
    return request;
  }

  /**
   * Get all {@link org.obiba.mica.access.domain.DataAccessRequest}s, optionally filtered by applicant.
   *
   * @param applicant
   * @return
   */
  public List<DataAccessRequest> findAll(@Nullable String applicant) {
    if(Strings.isNullOrEmpty(applicant)) return dataAccessRequestRepository.findAll();
    return dataAccessRequestRepository.findByApplicant(applicant);
  }

  //
  // Private methods
  //

  private void checkStatusTransition(DataAccessRequest.Status from, DataAccessRequest.Status to)
    throws IllegalArgumentException {
    if(from == to) return;

    switch(from) {
      case OPENED:
        if(to != DataAccessRequest.Status.SUBMITTED)
          throw new IllegalArgumentException("Opened data access request can only be submitted");
        break;
      case SUBMITTED:
        if(to != DataAccessRequest.Status.OPENED && to != DataAccessRequest.Status.REVIEWED)
          throw new IllegalArgumentException(
            "Submitted data access request can only be reopened or put under review");
        break;
      case REVIEWED:
        if(to != DataAccessRequest.Status.OPENED && to != DataAccessRequest.Status.APPROVED &&
          to != DataAccessRequest.Status.REJECTED) throw new IllegalArgumentException(
          "Reviewed data access request can only be reopened or be approved/rejected");
        break;
      case APPROVED:
        throw new IllegalArgumentException("Approved data access request cannot be modified");
      case REJECTED:
        throw new IllegalArgumentException("Rejected data access request cannot be modified");
    }
  }

}
