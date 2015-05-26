package org.obiba.mica.access.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.access.DataAccessRequestRepository;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.core.service.GitService;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.service.DataAccessFormService;
import org.obiba.mica.network.NoSuchNetworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;

@Service
@Validated
public class DataAccessRequestService {

  private static final Logger log = LoggerFactory.getLogger(DataAccessRequestService.class);

  @Inject
  private DataAccessRequestRepository dataAccessRequestRepository;

  @Inject
  private DataAccessFormService dataAccessFormService;

  @Inject
  private GitService gitService;

  public void save(@NotNull DataAccessRequest request) {
    DataAccessRequest saved = request;
    if(request.isNew()) {
      saved.setStatus(DataAccessRequest.Status.OPENED);
      //generateId(saved);
    } else {
      saved = dataAccessRequestRepository.findOne(request.getId());
      if(saved != null) {
        // validate the status
        saved.setStatus(request.getStatus());
        // merge beans
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

  public List<DataAccessRequest> findByStatus(@Nullable List<String> status) {
    if(status == null || status.size() == 0) return dataAccessRequestRepository.findAll();
    List<DataAccessRequest.Status> statusList =
      status.stream().map(s -> DataAccessRequest.Status.valueOf(s)).collect(Collectors.toList());

    return dataAccessRequestRepository.findAll().stream()
      .filter(dar -> statusList.contains(dar.getStatus())).collect(Collectors.toList());
  }

  //
  // Private methods
  //



  public byte[] getRequestPdf(String id, String lang) {
    DataAccessRequest dataAccessRequest = findById(id);
    DataAccessForm dataAccessForm = dataAccessFormService.findDataAccessForm().get();

    Locale locale = Locale.forLanguageTag(lang);
    Attachment pdfTemplate = dataAccessForm.getPdfTemplates().get(locale);

    byte[] template = gitService.readFileHead(dataAccessForm, pdfTemplate.getId());
    ByteArrayOutputStream ba = new ByteArrayOutputStream();

    try(PdfReaderAutoclosable reader = new PdfReaderAutoclosable(template);
        PdfStamperAutoclosable stamper = new PdfStamperAutoclosable(reader, ba)
    ) {
      fillPdfTemplateFromRequest(stamper, dataAccessRequest.getContent());
    } catch(IOException | DocumentException e) {
      throw new RuntimeException("Error creating data access request PDF", e);
    }

    return ba.toByteArray();
  }

  private void fillPdfTemplateFromRequest(PdfStamper stamper, String content) {
    stamper.setFormFlattening(true);

    AcroFields fields = stamper.getAcroFields();
    Map<String, Object> requestValues = fields.getFields().keySet().stream()
      .map(k -> getMapEntryFromContent(content, k))
      .filter(e -> e != null)
      .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

    requestValues.forEach((k, v) -> setField(fields, k, v));
  }

  private void setField(AcroFields fields, String key, Object value) {
    setField(fields, key, value.toString());
  }

  private void setField(AcroFields fields, String key, boolean value) {
    String[] states = fields.getAppearanceStates(key);
    setField(fields, key, states.length > 0 ? states[value ? 1 : 0] : states[0]);
  }

  private void setField(AcroFields fields, String key, String value) {
    try {
      fields.setField(key, value);
    } catch(DocumentException | IOException e) {
      throw new RuntimeException("Error setting PDF field", e);
    }
  }

  private Map.Entry<String, Object> getMapEntryFromContent(String content, String jsonPath) {
    try {
      return Maps.immutableEntry(jsonPath, JsonPath.read(content, jsonPath));
    } catch(InvalidPathException ex) {
      log.warn("Invalid json path in pdf template: {}", jsonPath);
    }

    return null;
  }

  private static class PdfReaderAutoclosable extends PdfReader implements AutoCloseable {
    public PdfReaderAutoclosable(byte[] pdfIn) throws IOException {
      super(pdfIn);
    }
  }

  private static class PdfStamperAutoclosable extends PdfStamper implements AutoCloseable {
    public PdfStamperAutoclosable (PdfReader reader, OutputStream os) throws IOException, DocumentException {
      super(reader, os);
    }
  }
}
