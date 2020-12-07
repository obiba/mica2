/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access.notification;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.obiba.mica.access.domain.*;
import org.obiba.mica.access.service.DataAccessAmendmentService;
import org.obiba.mica.access.service.DataAccessRequestService;
import org.obiba.mica.access.service.DataAccessRequestUtilService;
import org.obiba.mica.core.domain.AbstractAuditableDocument;
import org.obiba.mica.core.service.MailService;
import org.obiba.mica.micaConfig.domain.DataAccessForm;
import org.obiba.mica.micaConfig.service.DataAccessFormService;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.security.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A service to handle the notifications about the intermediate and final reports of a Data Access Request.
 */
@Service
@Validated
public class DataAccessRequestReportNotificationService {

  private static final Logger log = LoggerFactory.getLogger(DataAccessRequestReportNotificationService.class);

  private static final SimpleDateFormat ISO_8601 = new SimpleDateFormat("yyyy-MM-dd");

  private final DataAccessRequestService dataAccessRequestService;

  private final DataAccessAmendmentService dataAccessAmendmentService;

  private final DataAccessRequestUtilService dataAccessRequestUtilService;

  private final MailService mailService;

  private final MicaConfigService micaConfigService;

  private final DataAccessFormService dataAccessFormService;

  @Inject
  public DataAccessRequestReportNotificationService(DataAccessRequestService dataAccessRequestService, DataAccessAmendmentService dataAccessAmendmentService, DataAccessRequestUtilService dataAccessRequestUtilService, MailService mailService, MicaConfigService micaConfigService, DataAccessFormService dataAccessFormService) {
    this.dataAccessRequestService = dataAccessRequestService;
    this.dataAccessAmendmentService = dataAccessAmendmentService;
    this.dataAccessRequestUtilService = dataAccessRequestUtilService;
    this.mailService = mailService;
    this.micaConfigService = micaConfigService;
    this.dataAccessFormService = dataAccessFormService;
  }

  /**
   * Get the declared start and end date from the data access request or its amendments. Add intermediate
   * (yearly) milestone dates.
   *
   * @param dar
   * @return
   */
  public DataAccessRequestTimeline getReportsTimeline(DataAccessRequest dar) {
    DataAccessRequestTimeline timeline = new DataAccessRequestTimeline();
    timeline.setStartDate(getStartDate(dar));
    timeline.setEndDate(getEndDate(dar));
    // split time interval between start and end date by year steps
    if (timeline.hasStartDate() && timeline.hasEndDate() && timeline.getStartDate().before(timeline.getEndDate())) {
      LocalDate localStartDate = toLocalDate(timeline.getStartDate());
      LocalDate localEndDate = toLocalDate(timeline.getEndDate()).minus(6, ChronoUnit.MONTHS);
      LocalDate localDate = localStartDate.plus(1, ChronoUnit.YEARS);
      while (localDate.isBefore(localEndDate)) {
        timeline.addIntermediateDate(toDate(localDate));
        localDate = localDate.plus(1, ChronoUnit.YEARS);
      }
    }
    return timeline;
  }

  @Async
  @Scheduled(cron = "${dar.reminder.cron:0 0 0 * * ?}")
  public void remindDataAccessReports() {
    DataAccessForm dataAccessForm = dataAccessFormService.find().get();
    if (!dataAccessForm.isNotifyFinalReport() && !dataAccessForm.isNotifyIntermediateReport()) return;
    int nbOfDaysBeforeReport = dataAccessForm.getNbOfDaysBeforeReport();
    if (nbOfDaysBeforeReport < 0) return;

    LocalDate dateNow = LocalDate.now().minusDays(nbOfDaysBeforeReport);

    for (DataAccessRequest dar : dataAccessRequestService.findByStatus(Lists.newArrayList(DataAccessEntityStatus.APPROVED.name()))) {
      DataAccessRequestTimeline timeline = getReportsTimeline(dar);

      if (timeline.hasEndDate()) {
        LocalDate localEndDate = toLocalDate(timeline.getEndDate());
        if (dataAccessForm.isNotifyFinalReport() && dateNow.plusDays(nbOfDaysBeforeReport).equals(localEndDate)) {
          // today is the day to notify final report
          remindDataAccessFinalReport(dataAccessForm, dar, timeline.getEndDate(), nbOfDaysBeforeReport);
        } else if (dataAccessForm.isNotifyIntermediateReport() && timeline.hasIntermediateDates()) {
          for (Date interDate : timeline.getIntermediateDates()) {
            if (dateNow.plusDays(nbOfDaysBeforeReport).equals(toLocalDate(interDate))) {
              remindDataAccessIntermediateReport(dataAccessForm, dar, interDate, nbOfDaysBeforeReport);
              break;
            }
          }
        }
      } else {
        log.warn("No end date found for data access request {}", dar.getId());
      }
    }
  }

  private void remindDataAccessFinalReport(DataAccessForm dataAccessForm, DataAccessRequest request, Date reportDate, int nbOfDaysBeforeReport) {
    Map<String, String> ctx = getContext(request, reportDate, nbOfDaysBeforeReport);

    mailService.sendEmailToUsers(mailService.getSubject(dataAccessForm.getFinalReportSubject(), ctx,
      DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), "dataAccessRequestFinalReportApplicantEmail", ctx,
      request.getApplicant());

    mailService.sendEmailToGroups(mailService.getSubject(dataAccessForm.getFinalReportSubject(), ctx,
      DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), "dataAccessRequestFinalReportDAOEmail", ctx,
      Roles.MICA_DAO);
  }

  private void remindDataAccessIntermediateReport(DataAccessForm dataAccessForm, DataAccessRequest request, Date reportDate, int nbOfDaysBeforeReport) {
    Map<String, String> ctx = getContext(request, reportDate, nbOfDaysBeforeReport);

    mailService.sendEmailToUsers(mailService.getSubject(dataAccessForm.getIntermediateReportSubject(), ctx,
      DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), "dataAccessRequestIntermediateReportApplicantEmail", ctx,
      request.getApplicant());

    mailService.sendEmailToGroups(mailService.getSubject(dataAccessForm.getIntermediateReportSubject(), ctx,
      DataAccessRequestUtilService.DEFAULT_NOTIFICATION_SUBJECT), "dataAccessRequestIntermediateReportDAOEmail", ctx,
      Roles.MICA_DAO);
  }

  /**
   * Get data access request end date from a field in the data access amendments or in the original request, only if
   * the current status is "approved".
   *
   * @param dar
   * @return
   */
  private Date getEndDate(DataAccessRequest dar) {
    Date endDate = null;
    if (DataAccessEntityStatus.APPROVED.equals(dar.getStatus())) {
      endDate = getEndDateFromDataAccessAmendments(dar);
      if (endDate == null) {
        endDate = dataAccessRequestUtilService.getRequestEndDate(dar);
      }
    }
    return endDate;
  }

  /**
   * Get the data access request from a field in the data access request or from the approval data, only if
   * the current status is "approved".
   *
   * @param dar
   * @return
   */
  private Date getStartDate(DataAccessRequest dar) {
    return dar.getStartDateOrDefault();
  }

  private LocalDate toLocalDate(Date date) {
    return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
  }

  private Date toDate(LocalDate localDate) {
    return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  private Map<String, String> getContext(DataAccessRequest request, Date reportDate, int nbOfDaysBeforeReport) {
    Map<String, String> ctx = Maps.newHashMap();
    String organization = micaConfigService.getConfig().getName();
    String id = request.getId();
    String title = dataAccessRequestUtilService.getRequestTitle(request);

    ctx.put("organization", organization);
    ctx.put("publicUrl", micaConfigService.getPublicUrl());
    ctx.put("id", id);
    if (Strings.isNullOrEmpty(title)) title = id;
    ctx.put("title", title);
    ctx.put("applicant", request.getApplicant());
    ctx.put("status", request.getStatus().name());
    ctx.put("reportDate", ISO_8601.format(reportDate));
    ctx.put("nbOfDaysBeforeReport", nbOfDaysBeforeReport + "");

    return ctx;
  }

  private Date getEndDateFromDataAccessAmendments(DataAccessRequest dar) {
    List<DataAccessAmendment> accessAmendments = dataAccessAmendmentService.findByParentId(dar.getId()).stream()
      .filter(d -> DataAccessEntityStatus.APPROVED.equals(d.getStatus()))
      .sorted(Comparator.comparing(AbstractAuditableDocument::getLastModifiedDate).reversed()) // last modified first
      .collect(Collectors.toList());
    for (DataAccessAmendment daa : accessAmendments) {
      Date endDate = dataAccessRequestUtilService.getRequestEndDate(daa);
      if (endDate != null) return endDate;
    }
    return null;
  }

}
