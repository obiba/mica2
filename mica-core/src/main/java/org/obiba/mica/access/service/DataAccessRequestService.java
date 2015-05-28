package org.obiba.mica.access.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.access.DataAccessRequestRepository;
import org.obiba.mica.access.NoSuchDataAccessRequestException;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.core.service.GitService;
import org.obiba.mica.core.service.MailService;
import org.obiba.mica.file.Attachment;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.service.DataAccessFormService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.NoSuchNetworkException;
import org.obiba.mica.security.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.thymeleaf.spring4.SpringTemplateEngine;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;

@Service
@Validated
public class DataAccessRequestService {

  private static final Logger log = LoggerFactory.getLogger(DataAccessRequestService.class);

  private static final Configuration conf = Configuration.defaultConfiguration().addOptions(Option.ALWAYS_RETURN_LIST);

  @Inject
  private DataAccessRequestRepository dataAccessRequestRepository;

  @Inject
  private DataAccessFormService dataAccessFormService;

  @Inject
  private GitService gitService;

  @Inject
  private SpringTemplateEngine templateEngine;

  @Inject
  private MailService mailService;

  @Inject
  private MicaConfigService micaConfigService;

  @Value("classpath:config/data-access-form/data-access-request-template.pdf")
  private Resource defaultTemplateResource;

  public void save(@NotNull DataAccessRequest request) {
    DataAccessRequest saved = request;
    DataAccessRequest.Status from = null;
    if(request.isNew()) {
      saved.setStatus(DataAccessRequest.Status.OPENED);
      //generateId(saved);
    } else {
      saved = dataAccessRequestRepository.findOne(request.getId());
      if(saved != null) {
        from = saved.getStatus();
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

    sendNotificationEmails(saved, from);
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
    List<DataAccessRequest.Status> statusList = status.stream().map(s -> DataAccessRequest.Status.valueOf(s))
      .collect(Collectors.toList());

    return dataAccessRequestRepository.findAll().stream().filter(dar -> statusList.contains(dar.getStatus()))
      .collect(Collectors.toList());
  }

  //
  // Private methods
  //

  /**
   * Send a notification email, depending on the status of the request.
   *
   * @param request
   * @param from
   */
  private void sendNotificationEmails(DataAccessRequest request, @Nullable DataAccessRequest.Status from) {
    // check is new request
    if(from == null) return;

    Map<String, String> ctx = Maps.newHashMap();
    String organization = micaConfigService.getConfig().getName();
    ctx.put("organization", organization);
    ctx.put("publicUrl", micaConfigService.getPublicUrl());
    ctx.put("id", request.getId());
    ctx.put("title", request.getTitle());
    switch(request.getStatus()) {
      case SUBMITTED:
        mailService
          .sendEmailToUsers("[" + organization + "] Submitted: " + request.getTitle(), "dataAccessRequestSubmittedApplicantEmail", ctx,
            request.getApplicant());
        mailService
          .sendEmailToGroups("[" + organization + "] Submitted: " + request.getTitle(), "dataAccessRequestSubmittedDAOEmail", ctx,
            Roles.MICA_DAO);
        break;
      case REVIEWED:
        mailService
          .sendEmailToUsers("[" + organization + "] Reviewed: " + request.getTitle(), "dataAccessRequestReviewedApplicantEmail", ctx,
            request.getApplicant());
        break;
      case OPENED:
        mailService
          .sendEmailToUsers("[" + organization + "] Reopened: " + request.getTitle(), "dataAccessRequestReopenedApplicantEmail", ctx,
            request.getApplicant());
        break;
      case APPROVED:
        mailService
          .sendEmailToUsers("[" + organization + "] Approved: " + request.getTitle(), "dataAccessRequestApprovedApplicantEmail", ctx,
            request.getApplicant());
        break;
      case REJECTED:
        mailService
          .sendEmailToUsers("[" + organization + "] Rejected: " + request.getTitle(), "dataAccessRequestRejectedApplicantEmail", ctx,
            request.getApplicant());
        break;
    }
  }

  public byte[] getRequestPdf(String id, String lang) {
    DataAccessRequest dataAccessRequest = findById(id);
    ByteArrayOutputStream ba = new ByteArrayOutputStream();

    try(PdfReaderAutoclosable reader = new PdfReaderAutoclosable(getTemplate(Locale.forLanguageTag(lang)));
        PdfStamperAutoclosable stamper = new PdfStamperAutoclosable(reader, ba)
    ) {
      Object document = Configuration.defaultConfiguration().jsonProvider().parse(dataAccessRequest.getContent());
      fillPdfTemplateFromRequest(stamper, document);
    } catch(IOException | DocumentException e) {
      throw new RuntimeException("Error creating data access request PDF", e);
    }

    return ba.toByteArray();
  }

  private byte[] getTemplate(Locale locale) throws IOException {
    DataAccessForm dataAccessForm = dataAccessFormService.findDataAccessForm().get();
    Attachment pdfTemplate = dataAccessForm.getPdfTemplates().get(locale);
    byte[] template;

    if(pdfTemplate == null) {
      if(locale.equals(Locale.ROOT)) {
        Map<Locale, Attachment> pdfTemplates = dataAccessForm.getPdfTemplates();

        if(!pdfTemplates.isEmpty()) {
          pdfTemplate = dataAccessForm.getPdfTemplates().get(Locale.ENGLISH);

          if(pdfTemplate == null) pdfTemplate = dataAccessForm.getPdfTemplates().values().stream().findFirst().get();

          template = gitService.readFileHead(dataAccessForm, pdfTemplate.getId());
        } else template = ByteStreams.toByteArray(defaultTemplateResource.getInputStream());
      } else throw new NoSuchElementException();
    } else template = gitService.readFileHead(dataAccessForm, pdfTemplate.getId());

    return template;
  }

  private void fillPdfTemplateFromRequest(PdfStamper stamper, Object content) {
    stamper.setFormFlattening(true);

    AcroFields fields = stamper.getAcroFields();

    Map<String, List<Object>> requestValues = fields.getFields().keySet().stream()
      .map(k -> getMapEntryFromContent(content, k)).filter(e -> e != null && !e.getValue().isEmpty())
      .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

    requestValues.forEach((k, v) -> {
      Object value = v.get(0);
      if(value instanceof Boolean) setField(fields, k, (Boolean) value);
      else setField(fields, k, value);
    });
  }

  private void setField(AcroFields fields, String key, Object value) {
    setField(fields, key, value.toString());
  }

  private void setField(AcroFields fields, String key, Boolean value) {
    String[] states = fields.getAppearanceStates(key);
    if(states.length > 0) setField(fields, key, states.length > 1 ? states[value.booleanValue() ? 1 : 0] : states[0]);
  }

  private void setField(AcroFields fields, String key, String value) {
    try {
      fields.setField(key, value);
    } catch(DocumentException | IOException e) {
      throw new RuntimeException("Error setting PDF field", e);
    }
  }

  private Map.Entry<String, List<Object>> getMapEntryFromContent(Object content, String jsonPath) {
    try {
      List<Object> values = JsonPath.using(conf).parse(content).read(jsonPath);
      return Maps.immutableEntry(jsonPath, values);
    } catch(PathNotFoundException ex) {
      //ignore
    } catch(InvalidPathException e) {
      log.warn("Invalid jsonpath {}", jsonPath);
    }

    return null;
  }

  private static class PdfReaderAutoclosable extends PdfReader implements AutoCloseable {
    public PdfReaderAutoclosable(byte[] pdfIn) throws IOException {
      super(pdfIn);
    }
  }

  private static class PdfStamperAutoclosable extends PdfStamper implements AutoCloseable {
    public PdfStamperAutoclosable(PdfReader reader, OutputStream os) throws IOException, DocumentException {
      super(reader, os);
    }
  }

}
