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
import org.obiba.mica.access.domain.DataAccessAmendment;
import org.obiba.mica.access.domain.DataAccessEntity;
import org.obiba.mica.access.domain.DataAccessEntityStatus;
import org.obiba.mica.access.domain.DataAccessRequest;
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

  private Map<DataAccessRequest, List<DataAccessAmendment>> dataAccessRequestDtos;
  private String lang;

  private DocumentContext darSchema;
  private DocumentContext amdSchema;

  public CsvReportGenerator(Map<DataAccessRequest, List<DataAccessAmendment>> dataAccessRequestDtos,
                            String darSchemaAsString,
                            String amdSchemaAsString,
                            String lang) {

    this.dataAccessRequestDtos = dataAccessRequestDtos;
    this.lang = lang;

    darSchema = JsonPath.parse(darSchemaAsString);
    amdSchema = JsonPath.parse(amdSchemaAsString);
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

    writer.writeNext(toArray(extractTranslatedField(darSchema, GENERIC_TANSLATION_PREFIX + ".detailedOverview")));
    Set<String> tableKeys = darSchema.read("table", Map.class).keySet();

    dataAccessRequestDtos.entrySet().forEach(entry -> {
      writer.writeNext(tableKeys.stream()
        .map(key -> extractTranslatedField(darSchema, "table.['" + key + "']"))
        .toArray(String[]::new));

      DataAccessRequest dataAccessRequestDto = entry.getKey();
      DocumentContext dataAccessRequestContent = JsonPath.parse(dataAccessRequestDto.getContent());

      addGenericVariablesInDocumentContext(dataAccessRequestDto, darSchema, dataAccessRequestContent);

      writer.writeNext(
        tableKeys.stream()
          .map(key -> extractValueFromDataAccessRequest(dataAccessRequestContent, key))
          .toArray(String[]::new));

      writer.writeNext(toArray(""));
      List<DataAccessAmendment> amendments = entry.getValue();
      if (amendments != null && !amendments.isEmpty()) {
        writeEachAmendment(amendments, writer);
      }

      writer.writeNext(toArray(""));
      writer.writeNext(toArray(""));
      writer.writeNext(toArray(""));
    });
  }

  private void writeEachAmendment(List<DataAccessAmendment> amendments, CSVWriter writer) {
    Set<String> tableKeys = amdSchema.read("table", Map.class).keySet();
    writer.writeNext(tableKeys.stream()
      .map(key -> extractTranslatedField(amdSchema, "table.['" + key + "']"))
      .toArray(String[]::new));

    for (DataAccessAmendment amendment : amendments) {
      DocumentContext amendmentContent = JsonPath.parse(amendment.getContent());

      addGenericVariablesInDocumentContext(amendment, amdSchema, amendmentContent);

      writer.writeNext(
        tableKeys.stream()
          .map(key -> extractValueFromDataAccessRequest(amendmentContent, key))
          .toArray(String[]::new));
    }
  }

  private void addGenericVariablesInDocumentContext(DataAccessEntity dataAccessEntity, DocumentContext context, DocumentContext dataAccessRequestContent) {
    dataAccessRequestContent.put("$", GENERIC_VARIALES_PREFIX, new HashMap<>());
    dataAccessRequestContent.put(GENERIC_VARIALES_PREFIX, "status", extractTranslatedField(context, dataAccessEntity.getStatus()));
    dataAccessRequestContent.put(GENERIC_VARIALES_PREFIX, "creationDate", formatDate(dataAccessEntity.getCreatedDate()));
    dataAccessRequestContent.put(GENERIC_VARIALES_PREFIX, "accessRequestId", dataAccessEntity.getId());

    DateTime lastApprovedOrRejectDate = extractLastApprovedOrRejectDate(dataAccessEntity.getStatusChangeHistory());
    DateTime firstSubmissionDate = extractFirstSubmissionDate(dataAccessEntity.getStatusChangeHistory());
    Integer numberOfDaysBetweenSubmissionAndApproveOrReject = calculateDaysBetweenDates(lastApprovedOrRejectDate, firstSubmissionDate);
    dataAccessRequestContent.put(GENERIC_VARIALES_PREFIX, "lastApprovedOrRejectedDate", lastApprovedOrRejectDate != null ? formatDate(lastApprovedOrRejectDate) : EMPTY_CELL_CONTENT);
    dataAccessRequestContent.put(GENERIC_VARIALES_PREFIX, "firstSubmissionDate", firstSubmissionDate != null ? formatDate(firstSubmissionDate) : EMPTY_CELL_CONTENT);
    dataAccessRequestContent.put(GENERIC_VARIALES_PREFIX, "numberOfDaysBetweenFirstSubmissionAndApproveOrReject", numberOfDaysBetweenSubmissionAndApproveOrReject != null ? numberOfDaysBetweenSubmissionAndApproveOrReject : EMPTY_CELL_CONTENT);
  }

  private void writeSummary(CSVWriter writer) {
    writer.writeNext(toArray(extractTranslatedField(darSchema, GENERIC_TANSLATION_PREFIX + ".summary")));
    writer.writeNext(toArray(extractTranslatedField(darSchema, GENERIC_TANSLATION_PREFIX + ".currentStatus"), extractTranslatedField(darSchema, GENERIC_TANSLATION_PREFIX + ".numberOfAccessRequests")));

    Map<DataAccessEntityStatus, Long> summaryStatistics = dataAccessRequestDtos.keySet().stream().collect(Collectors.groupingBy(DataAccessRequest::getStatus, Collectors.counting()));
    writer.writeNext(toArray(extractTranslatedField(darSchema, DataAccessEntityStatus.OPENED), getWith0AsDefault(summaryStatistics.get(DataAccessEntityStatus.OPENED))));
    writer.writeNext(toArray(extractTranslatedField(darSchema, DataAccessEntityStatus.SUBMITTED), getWith0AsDefault(summaryStatistics.get(DataAccessEntityStatus.SUBMITTED))));
    writer.writeNext(toArray(extractTranslatedField(darSchema, DataAccessEntityStatus.REVIEWED), getWith0AsDefault(summaryStatistics.get(DataAccessEntityStatus.REVIEWED))));
    writer.writeNext(toArray(extractTranslatedField(darSchema, DataAccessEntityStatus.APPROVED), getWith0AsDefault(summaryStatistics.get(DataAccessEntityStatus.APPROVED))));
    writer.writeNext(toArray(extractTranslatedField(darSchema, DataAccessEntityStatus.CONDITIONALLY_APPROVED), getWith0AsDefault(summaryStatistics.get(DataAccessEntityStatus.CONDITIONALLY_APPROVED))));
    writer.writeNext(toArray(extractTranslatedField(darSchema, DataAccessEntityStatus.REJECTED), getWith0AsDefault(summaryStatistics.get(DataAccessEntityStatus.REJECTED))));
    writer.writeNext(toArray(""));
  }

  private void writeHeader(CSVWriter writer) {
    writer.writeNext(toArray(extractTranslatedField(darSchema, GENERIC_TANSLATION_PREFIX + ".title")));
    writer.writeNext(toArray(extractTranslatedField(darSchema, GENERIC_TANSLATION_PREFIX + ".subtitle")));
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
    return extractTranslatedField(darSchema, GENERIC_TANSLATION_PREFIX + "." + translationKey);
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

  private String extractTranslatedField(DocumentContext context, DataAccessEntityStatus status) {
    switch (status) {
      case OPENED:
        return extractTranslatedField(context, GENERIC_TANSLATION_PREFIX + ".opened");
      case SUBMITTED:
        return extractTranslatedField(context, GENERIC_TANSLATION_PREFIX + ".submitted");
      case REVIEWED:
        return extractTranslatedField(context, GENERIC_TANSLATION_PREFIX + ".underReview");
      case APPROVED:
        return extractTranslatedField(context, GENERIC_TANSLATION_PREFIX + ".approved");
      case CONDITIONALLY_APPROVED:
        return extractTranslatedField(context, GENERIC_TANSLATION_PREFIX + ".conditionallyApproved");
      case REJECTED:
        return extractTranslatedField(context, GENERIC_TANSLATION_PREFIX + ".rejected");
      default:
        throw new IllegalStateException(String.format("Impossible to map the status [%s] to variable", status));
    }
  }

  // For Testing
  String extractTranslatedField(String fieldPath) {
    return extractTranslatedField(darSchema, fieldPath);
  }

  String extractTranslatedField(DocumentContext context, String fieldPath) {
    try {
      return context.read(fieldPath + "." + lang, String.class);
    } catch (PathNotFoundException e) {
      return context.read(fieldPath + "." + DEFAULT_LANGUAGE, String.class);
    }
  }

  DateTime extractLastApprovedOrRejectDate(List<StatusChange> statusChangeHistory) {
    return statusChangeHistory.stream()
      .filter(statusChange -> asList(DataAccessEntityStatus.APPROVED, DataAccessEntityStatus.REJECTED).contains(statusChange.getTo()))
      .map(StatusChange::getChangedOn)
      .max(AbstractInstant::compareTo)
      .orElseGet(() -> null);
  }

  DateTime extractFirstSubmissionDate(List<StatusChange> statusChangeHistory) {
    return statusChangeHistory.stream()
      .filter(statusChange -> DataAccessEntityStatus.SUBMITTED.equals(statusChange.getTo()))
      .map(StatusChange::getChangedOn)
      .min(AbstractInstant::compareTo)
      .orElseGet(() -> null);
  }
}
