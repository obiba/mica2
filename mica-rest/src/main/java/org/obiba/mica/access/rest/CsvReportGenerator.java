/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access.rest;

import au.com.bytecode.opencsv.CSVWriter;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.base.AbstractInstant;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.domain.DataAccessRequest.Status;
import org.obiba.mica.access.domain.StatusChange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class CsvReportGenerator {

  public static final String DATETIME_FORMAT = "dd/MM/YYYY";

  private static final String DEFAULT_LANGUAGE = "en";
  private static final String GENERIC_VARIALES_PREFIX = "generic";
  private static final String GENERIC_TANSLATION_PREFIX = "headers";
  private static final String EMPTY_CELL_CONTENT = "N/A";

  private List<DataAccessRequest> dataAccessRequestDtos;
  private String lang;

  private DocumentContext csvSchema;

  public CsvReportGenerator(List<DataAccessRequest> dataAccessRequestDtos, String csvSchemaAsString, String lang) {
    this.dataAccessRequestDtos = dataAccessRequestDtos;
    this.lang = lang;

    csvSchema = JsonPath.parse(csvSchemaAsString);
  }

  public void write(OutputStream outputStream) {

    try (CSVWriter writer = new CSVWriter(new PrintWriter(outputStream))) {

      writeHeader(writer);
      writeSummary(writer);
      writeEachDataAccessRequest(writer);

    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void writeEachDataAccessRequest(CSVWriter writer) {

    writer.writeNext(toArray(extractTranslatedField(GENERIC_TANSLATION_PREFIX + ".detailedOverview")));

    Set<String> tableKeys = csvSchema.read("table", Map.class).keySet();
    writer.writeNext(tableKeys.stream()
      .map(key -> extractTranslatedField("table.['" + key + "']"))
      .toArray(String[]::new));

    for (DataAccessRequest dataAccessRequestDto : dataAccessRequestDtos) {

      DocumentContext dataAccessRequestContent = JsonPath.parse(dataAccessRequestDto.getContent());

      addGenericVariablesInDocumentContext(dataAccessRequestDto, dataAccessRequestContent);

      writer.writeNext(
        tableKeys.stream()
          .map(key -> extractValueFromDataAccessRequest(dataAccessRequestContent, key))
          .toArray(String[]::new));
    }
  }

  private void addGenericVariablesInDocumentContext(DataAccessRequest dataAccessRequest, DocumentContext dataAccessRequestContent) {
    dataAccessRequestContent.put("$", GENERIC_VARIALES_PREFIX, new HashMap<>());
    dataAccessRequestContent.put(GENERIC_VARIALES_PREFIX, "status", extractTranslatedField(dataAccessRequest.getStatus()));
    dataAccessRequestContent.put(GENERIC_VARIALES_PREFIX, "creationDate", formatDate(dataAccessRequest.getCreatedDate()));
    dataAccessRequestContent.put(GENERIC_VARIALES_PREFIX, "accessRequestId", dataAccessRequest.getId());

    DateTime lastApprovedOrRejectDate = extractLastApprovedOrRejectDate(dataAccessRequest.getStatusChangeHistory());
    DateTime firstSubmissionDate = extractFirstSubmissionDate(dataAccessRequest.getStatusChangeHistory());
    Integer numberOfDaysBetweenSubmissionAndApproveOrReject = calculateDaysBetweenDates(lastApprovedOrRejectDate, firstSubmissionDate);
    dataAccessRequestContent.put(GENERIC_VARIALES_PREFIX, "lastApprovedOrRejectedDate", lastApprovedOrRejectDate != null ? formatDate(lastApprovedOrRejectDate) : EMPTY_CELL_CONTENT);
    dataAccessRequestContent.put(GENERIC_VARIALES_PREFIX, "firstSubmissionDate", firstSubmissionDate != null ? formatDate(firstSubmissionDate) : EMPTY_CELL_CONTENT);
    dataAccessRequestContent.put(GENERIC_VARIALES_PREFIX, "numberOfDaysBetweenFirstSubmissionAndApproveOrReject", numberOfDaysBetweenSubmissionAndApproveOrReject != null ? numberOfDaysBetweenSubmissionAndApproveOrReject : EMPTY_CELL_CONTENT);
  }

  private void writeSummary(CSVWriter writer) {
    writer.writeNext(toArray(extractTranslatedField(GENERIC_TANSLATION_PREFIX + ".summary")));
    writer.writeNext(toArray(extractTranslatedField(GENERIC_TANSLATION_PREFIX + ".currentStatus"), extractTranslatedField(GENERIC_TANSLATION_PREFIX + ".numberOfAccessRequests")));

    Map<Status, Long> summaryStatistics = dataAccessRequestDtos.stream().collect(Collectors.groupingBy(DataAccessRequest::getStatus, Collectors.counting()));
    writer.writeNext(toArray(extractTranslatedField(Status.OPENED), getWith0AsDefault(summaryStatistics.get(Status.OPENED))));
    writer.writeNext(toArray(extractTranslatedField(Status.SUBMITTED), getWith0AsDefault(summaryStatistics.get(Status.SUBMITTED))));
    writer.writeNext(toArray(extractTranslatedField(Status.REVIEWED), getWith0AsDefault(summaryStatistics.get(Status.REVIEWED))));
    writer.writeNext(toArray(extractTranslatedField(Status.APPROVED), getWith0AsDefault(summaryStatistics.get(Status.APPROVED))));
    writer.writeNext(toArray(extractTranslatedField(Status.CONDITIONALLY_APPROVED), getWith0AsDefault(summaryStatistics.get(Status.CONDITIONALLY_APPROVED))));
    writer.writeNext(toArray(extractTranslatedField(Status.REJECTED), getWith0AsDefault(summaryStatistics.get(Status.REJECTED))));
    writer.writeNext(toArray(""));
  }

  private void writeHeader(CSVWriter writer) {
    writer.writeNext(toArray(extractTranslatedField(GENERIC_TANSLATION_PREFIX + ".title")));
    writer.writeNext(toArray(extractTranslatedField(GENERIC_TANSLATION_PREFIX + ".subtitle")));
    writer.writeNext(toArray(formatDate(new DateTime())));
    writer.writeNext(toArray(""));
  }

  private Integer calculateDaysBetweenDates(DateTime lastApprovedOrRejectDate, DateTime firstSubmissionDate) {
    if (lastApprovedOrRejectDate == null || firstSubmissionDate == null)
      return null;
    return Days.daysBetween(firstSubmissionDate, lastApprovedOrRejectDate).getDays();
  }

  private String extractValueFromDataAccessRequest(DocumentContext dataAccessRequestDetails, String key) {
    try {
      try {
        Boolean booleanValue = dataAccessRequestDetails.read(key, Boolean.class);
        return translateBooleanValue(booleanValue);
      } catch (ClassCastException ignore) {
      }
      return dataAccessRequestDetails.read(key, String.class);

    } catch (PathNotFoundException e) {
      return EMPTY_CELL_CONTENT;
    }
  }

  private String translateBooleanValue(Boolean value) {
    if (value == null)
      return EMPTY_CELL_CONTENT;

    String translationKey = value ? "true" : "false";
    return extractTranslatedField(GENERIC_TANSLATION_PREFIX + "." + translationKey);
  }

  private String getWith0AsDefault(Long value) {
    return value != null ? value.toString() : "0";
  }

  private String[] toArray(String... elements) {
    return elements;
  }

  private String formatDate(DateTime dateTime) {
    return dateTime.toString(DATETIME_FORMAT);
  }

  private String extractTranslatedField(Status status) {
    switch (status) {
      case OPENED:
        return extractTranslatedField(GENERIC_TANSLATION_PREFIX + ".opened");
      case SUBMITTED:
        return extractTranslatedField(GENERIC_TANSLATION_PREFIX + ".submitted");
      case REVIEWED:
        return extractTranslatedField(GENERIC_TANSLATION_PREFIX + ".underReview");
      case APPROVED:
        return extractTranslatedField(GENERIC_TANSLATION_PREFIX + ".approved");
      case CONDITIONALLY_APPROVED:
        return extractTranslatedField(GENERIC_TANSLATION_PREFIX + ".conditionallyApproved");
      case REJECTED:
        return extractTranslatedField(GENERIC_TANSLATION_PREFIX + ".rejected");
      default:
        throw new IllegalStateException(String.format("Impossible to map the status [%s] to variable", status));
    }
  }

  String extractTranslatedField(String fieldPath) {
    try {
      return csvSchema.read(fieldPath + "." + lang, String.class);
    } catch (PathNotFoundException e) {
      return csvSchema.read(fieldPath + "." + DEFAULT_LANGUAGE, String.class);
    }
  }

  DateTime extractLastApprovedOrRejectDate(List<StatusChange> statusChangeHistory) {
    return statusChangeHistory.stream()
      .filter(statusChange -> asList(Status.APPROVED, Status.REJECTED).contains(statusChange.getTo()))
      .map(StatusChange::getChangedOn)
      .max(AbstractInstant::compareTo)
      .orElseGet(() -> null);
  }

  DateTime extractFirstSubmissionDate(List<StatusChange> statusChangeHistory) {
    return statusChangeHistory.stream()
      .filter(statusChange -> Status.SUBMITTED.equals(statusChange.getTo()))
      .map(StatusChange::getChangedOn)
      .min(AbstractInstant::compareTo)
      .orElseGet(() -> null);
  }
}
