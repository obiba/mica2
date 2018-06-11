package org.obiba.mica.access.rest;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.base.Strings;
import org.joda.time.DateTime;
import org.obiba.core.translator.Translator;
import org.obiba.mica.access.domain.ActionLog;
import org.obiba.mica.access.domain.ChangeLog;
import org.obiba.mica.access.domain.DataAccessAmendment;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.domain.StatusChange;
import org.obiba.mica.search.csvexport.CsvReportGenerator;
import org.obiba.mica.user.UserProfileService;
import org.obiba.shiro.realm.ObibaRealm;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CsvHistoryReportGenerator implements CsvReportGenerator {
  private final Map<DataAccessRequest, List<DataAccessAmendment>> dataAccessRequestListMap;
  private final Translator translator;
  private final UserProfileService userProfileService;
  private final Locale locale;

  public CsvHistoryReportGenerator(Map<DataAccessRequest, List<DataAccessAmendment>> list,
                                   Translator translator,
                                   Locale locale,
                                   UserProfileService userProfileService) {
    dataAccessRequestListMap = list;
    this.translator = translator;
    this.locale = locale;
    this.userProfileService = userProfileService;
  }

  @Override
  public void write(OutputStream outputStream) {
    try (CSVWriter writer = new CSVWriter(new PrintWriter(outputStream))) {
      writeHeader(writer);
      writeBody(writer);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
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

          List<Node> dataAccessActionHistory = dar
            .getActionLogHistory()
            .stream()
            .map(action -> new Node(dar.getId(), "data-access-request.action-log.title", action))
            .collect(Collectors.toList());

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
      return changeLog.getChangedOn().toString("MMM d yyyy " + timeFormat, locale);
    }

    String getStatus() {
      return changeLog instanceof StatusChange
        ? ((StatusChange)changeLog).getTo().toString().toUpperCase()
        : ((ActionLog)changeLog).getAction();
    }

    DateTime getChangedOn() {
      return changeLog.getChangedOn();
    }

    String getAuthor() {
      String author = changeLog.getAuthor();
      ObibaRealm.Subject profile = userProfileService.getProfile(author);
      Map<String, String> nameMap = profile.getAttributes()
        .stream()
        .filter(map -> map.containsValue("firstName") || map.containsValue("lastName"))
        .collect(Collectors.toMap(e -> e.get("key"), e -> e.get("value")));

      String firstName = Strings.emptyToNull(nameMap.get("firstName"));
      String lastName = Strings.emptyToNull(nameMap.get("lastName"));

      return firstName == null || lastName == null ? author : firstName  + " " + lastName;
    }
  }

}
