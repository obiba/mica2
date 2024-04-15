package org.obiba.mica.access.rest;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.obiba.core.translator.Translator;
import org.obiba.mica.access.domain.ActionLog;
import org.obiba.mica.access.domain.ChangeLog;
import org.obiba.mica.access.domain.DataAccessAmendment;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.domain.StatusChange;
import org.obiba.mica.search.reports.ReportGenerator;
import org.obiba.mica.user.UserProfileService;
import org.obiba.shiro.realm.ObibaRealm;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CsvHistoryReportGenerator implements ReportGenerator {
  private final Map<DataAccessRequest, List<DataAccessAmendment>> dataAccessRequestListMap;
  private final Translator translator;
  private final UserProfileService userProfileService;
  private final Locale locale;
  private final boolean includeActions;

  public CsvHistoryReportGenerator(Map<DataAccessRequest, List<DataAccessAmendment>> list,
                                   Translator translator,
                                   Locale locale,
                                   UserProfileService userProfileService,
                                   boolean includeActions) {
    dataAccessRequestListMap = list;
    this.translator = translator;
    this.locale = locale;
    this.userProfileService = userProfileService;
    this.includeActions = includeActions;
  }

  @Override
  public void write(OutputStream outputStream, boolean omitHeader) {
    try (CSVWriter writer = new CSVWriter(new PrintWriter(outputStream))) {
      if (!omitHeader) writeHeader(writer);
      writeBody(writer);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void write(OutputStream outputStream) {
    write(outputStream, false);
  }

  private String[] toArray(String... elements) {
    return elements;
  }

  private void writeHeader(CSVWriter writer) {
    writer.writeNext(
      toArray(
        translator.translate("id"),
        translator.translate("type"),
        translator.translate("status"),
        translator.translate("changed-by"),
        translator.translate("changed-on")
      )
    );
  }

  private void writeBody(CSVWriter writer) {
    prepare().forEach(node ->
      writer.writeNext(toArray(
        node.id,
        translator.translate(node.type),
        translator.translate(node.getStatus()),
        node.getAuthor(),
        node.getDate()
      ))
    );
  }

  private List<Node> prepare() {
    return dataAccessRequestListMap.entrySet()
      .stream()
      .map(entry -> {
          List<Node> amendmentStatusHistory = entry.getValue()
            .stream()
            .flatMap(amd -> amd.getStatusChangeHistory().stream()
              .map(status -> new Node(amd.getId(), "data-access-amendment.title", status))
            ).collect(Collectors.toList());

          DataAccessRequest dar = entry.getKey();
          List<Node> dataAccessStatusHistory = dar
            .getStatusChangeHistory()
            .stream()
            .map(status -> new Node(dar.getId(), "data-access-request.title", status))
            .collect(Collectors.toList());

          List<Node> dataAccessActionHistory =
            includeActions
              ? dar.getActionLogHistory()
                .stream()
                .map(action -> new Node(dar.getId(), "data-access-request.action-log.title", action))
                .collect(Collectors.toList())
              : Lists.newArrayList();

        return Stream.of(dataAccessStatusHistory, dataAccessActionHistory, amendmentStatusHistory)
            .flatMap(list -> list.stream())
            .sorted(Comparator.comparing(Node::getChangedOn).reversed())
            .collect(Collectors.toList());
        }
      )
      .flatMap(nodes -> nodes.stream())
      .sorted(Comparator.comparing(node -> node.id))
      .collect(Collectors.toList());
  }

  private class Node {
    private String id;
    private String type;
    private ChangeLog changeLog;

    Node(String id, String type, ChangeLog changeLog) {
      this.id = id;
      this.type = type;
      this.changeLog = changeLog;
    }

    String getDate() {
      String timeFormat = locale.getLanguage().equalsIgnoreCase("en") ? "hh:mm a" : "HH:mm";
      return DateTimeFormatter.ofPattern("MMM d yyyy " + timeFormat, locale).format(changeLog.getChangedOn());
    }

    String getStatus() {
      return changeLog instanceof StatusChange
        ? ((StatusChange)changeLog).getTo().toString().toUpperCase()
        : ((ActionLog)changeLog).getAction();
    }

    LocalDateTime getChangedOn() {
      return changeLog.getChangedOn();
    }

    String getAuthor() {
      String author = changeLog.getAuthor();
      ObibaRealm.Subject profile = userProfileService.getProfile(author);
      List<Map<String, String>> attributes = profile.getAttributes();
      String fullName = author;

      if (attributes != null) {
        Map<String, String> nameMap = attributes.stream()
          .filter(map -> map.containsValue("firstName") || map.containsValue("lastName"))
          .collect(Collectors.toMap(e -> e.get("key"), e -> e.get("value")));

        String firstName = Strings.emptyToNull(nameMap.get("firstName"));
        String lastName = Strings.emptyToNull(nameMap.get("lastName"));

        if (firstName!= null && lastName != null) {
          fullName = firstName  + " " + lastName;
        }
      }

      return fullName;
    }
  }

}
