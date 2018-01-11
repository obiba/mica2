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

import com.jayway.jsonpath.JsonPathException;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.junit.Test;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.access.domain.DataAccessRequest.Status;
import org.obiba.mica.access.domain.StatusChange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CsvReportGeneratorTest {

  @Test
  public void when_translation_exists__take_it() throws JSONException {
    CsvReportGenerator frenchReportGenerator = new CsvReportGenerator(null, givenValidCsvWithSomeFrenchTranslations(), "fr");

    assertThat(frenchReportGenerator.extractTranslatedField("headers.title"), is("OK"));
    assertThat(frenchReportGenerator.extractTranslatedField("headers.subtitle"), is("OK"));
    assertThat(frenchReportGenerator.extractTranslatedField("headers.summary"), is("OK"));
  }

  @Test(expected = JsonPathException.class)
  public void when_use_invalid_json_schema__throws_exception() {

    new CsvReportGenerator(Collections.emptyList(), "{", "en").write(new ByteArrayOutputStream());
  }

  @Test(expected = JsonPathException.class)
  public void when_use_valid_schema_with_missing_translations__throws_exception() {

    String csvSchema = "{\"headers\":" +
      "   {\"organizationName\":{\"en\":\"Maelstrom\"}}" +
      "}";

    new CsvReportGenerator(Collections.emptyList(), csvSchema, "en").write(new ByteArrayOutputStream());
  }

  @Test
  public void can_extract_last_approved_or_rejected_date() {

    CsvReportGenerator csvReportGenerator = new CsvReportGenerator(null, "{}", null);
    List<StatusChange> statusChangeHistory = asList(
      StatusChange.newBuilder().changedOn(new DateTime(2000, 1, 1, 1, 1)).current(Status.APPROVED).build(),
      StatusChange.newBuilder().changedOn(new DateTime(2001, 1, 1, 1, 1)).current(Status.APPROVED).build(),
      StatusChange.newBuilder().changedOn(new DateTime(2016, 1, 1, 1, 1)).current(Status.OPENED).build(),
      StatusChange.newBuilder().changedOn(new DateTime(2015, 1, 1, 1, 1)).current(Status.APPROVED).build(),
      StatusChange.newBuilder().changedOn(new DateTime(2002, 1, 1, 1, 1)).current(Status.APPROVED).build(),
      StatusChange.newBuilder().changedOn(new DateTime(2003, 1, 1, 1, 1)).current(Status.APPROVED).build()
    );

    DateTime lastApprovedOrRejectedDate = csvReportGenerator.extractLastApprovedOrRejectDate(statusChangeHistory);

    assertThat(lastApprovedOrRejectedDate, is(new DateTime(2015, 1, 1, 1, 1)));
  }

  @Test
  public void can_extract_first_submitted_date() {

    CsvReportGenerator csvReportGenerator = new CsvReportGenerator(null, "{}", null);
    List<StatusChange> statusChangeHistory = asList(
      StatusChange.newBuilder().changedOn(new DateTime(2000, 1, 1, 1, 1)).current(Status.SUBMITTED).build(),
      StatusChange.newBuilder().changedOn(new DateTime(2001, 1, 1, 1, 1)).current(Status.SUBMITTED).build(),
      StatusChange.newBuilder().changedOn(new DateTime(1900, 1, 1, 1, 1)).current(Status.OPENED).build(),
      StatusChange.newBuilder().changedOn(new DateTime(1901, 1, 1, 1, 1)).current(Status.SUBMITTED).build(),
      StatusChange.newBuilder().changedOn(new DateTime(2002, 1, 1, 1, 1)).current(Status.SUBMITTED).build(),
      StatusChange.newBuilder().changedOn(new DateTime(2003, 1, 1, 1, 1)).current(Status.SUBMITTED).build()
    );

    DateTime firstSubmissionDate = csvReportGenerator.extractFirstSubmissionDate(statusChangeHistory);

    assertThat(firstSubmissionDate, is(new DateTime(1901, 1, 1, 1, 1)));
  }

  @Test
  public void can_generate_report() throws IOException {

    String csvSchema = givenValidCsvSchema();
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    List<DataAccessRequest> dataAccessRequest = new ArrayList<>();

    dataAccessRequest.add(openedDataAccessRequest());
    dataAccessRequest.add(submittedDataAccessRequest());
    dataAccessRequest.add(approvedDataAccessRequest());

    new CsvReportGenerator(dataAccessRequest, csvSchema, "fr").write(byteArrayOutputStream);

    assertThat(byteArrayOutputStream.toString(), is(expectedResult()));
  }

  private DataAccessRequest openedDataAccessRequest() {
    DataAccessRequest dataAccessRequest = DataAccessRequest.newBuilder()
      .status(Status.OPENED.toString())
      .content("{'title':'Opened request title'}")
      .build();
    dataAccessRequest.setId("refused id");
    dataAccessRequest.setCreatedDate(new DateTime(2016, 8, 20, 14, 36));
    dataAccessRequest.setLastModifiedDate(new DateTime(2016, 8, 25, 14, 36));
    dataAccessRequest.setStatusChangeHistory(Collections.singletonList(
      StatusChange.newBuilder().changedOn(new DateTime(2016, 6, 1, 1, 1)).current(Status.SUBMITTED).build()));
    return dataAccessRequest;
  }

  private DataAccessRequest submittedDataAccessRequest() {
    DataAccessRequest dataAccessRequest = DataAccessRequest.newBuilder()
      .status(Status.SUBMITTED.toString())
      .content("{'title':'Opened request title', 'isWaiting':true}")
      .build();
    dataAccessRequest.setId("refused id");
    dataAccessRequest.setCreatedDate(new DateTime(2016, 8, 20, 14, 36));
    dataAccessRequest.setLastModifiedDate(new DateTime(2016, 8, 25, 14, 36));
    dataAccessRequest.setStatusChangeHistory(Collections.singletonList(
      StatusChange.newBuilder().changedOn(new DateTime(2016, 6, 1, 1, 1)).current(Status.SUBMITTED).build()));
    return dataAccessRequest;
  }

  private DataAccessRequest approvedDataAccessRequest() {
    DataAccessRequest dataAccessRequest = DataAccessRequest.newBuilder()
      .status(Status.APPROVED.toString())
      .content("{'title':'Approved request title', 'isWaiting':false}")
      .build();
    dataAccessRequest.setId("approved id");
    dataAccessRequest.setCreatedDate(new DateTime(2016, 8, 20, 14, 36));
    dataAccessRequest.setLastModifiedDate(new DateTime(2015, 8, 25, 14, 36));
    dataAccessRequest.setStatusChangeHistory(asList(
      StatusChange.newBuilder().changedOn(new DateTime(2017, 1, 1, 1, 1)).current(Status.APPROVED).build(),
      StatusChange.newBuilder().changedOn(new DateTime(2016, 6, 1, 1, 1)).current(Status.SUBMITTED).build()));
    return dataAccessRequest;
  }

  private String givenValidCsvWithSomeFrenchTranslations() {
    return "{\n" +
      "        \"headers\": {" +
      "        \"title\": {" +
      "            \"en\": \"ERROR\"," +
      "            \"fr\": \"OK\"" +
      "        }," +
      "        \"subtitle\": {" +
      "            \"fr\": \"OK\"" +
      "        }," +
      "        \"summary\": {\n" +
      "            \"en\": \"OK\"" +
      "        }" +
      "     }" +
      " }";
  }

  private String givenValidCsvSchema() {
    return "{\n" +
      "  \"headers\": {\n" +
      "    \"title\": {\n" +
      "      \"en\": \"<Organisation> Access Office\",\n" +
      "      \"fr\": \"<Organisation> Access Office\"\n" +
      "    },\n" +
      "    \"subtitle\": {\n" +
      "      \"en\": \"Access Requests Report\",\n" +
      "      \"fr\": \"Rapport des demandes d'accès\"\n" +
      "    },\n" +
      "    \"summary\": {\n" +
      "      \"en\": \"SUMMARY\",\n" +
      "      \"fr\": \"SOMMAIRE\"\n" +
      "    },\n" +
      "    \"currentStatus\": {\n" +
      "      \"en\": \"CURRENT STATUS\",\n" +
      "      \"fr\": \"STATUT ACTUEL\"\n" +
      "    },\n" +
      "    \"numberOfAccessRequests\": {\n" +
      "      \"en\": \"NUMBER OF ACCESS REQUESTS\",\n" +
      "      \"fr\": \"NOMBRE DE DEMANDES D'ACCÈS\"\n" +
      "    },\n" +
      "    \"opened\": {\n" +
      "      \"en\": \"Opened\",\n" +
      "      \"fr\": \"Ouvert\"\n" +
      "    },\n" +
      "    \"submitted\": {\n" +
      "      \"en\": \"submitted\",\n" +
      "      \"fr\": \"submitted\"\n" +
      "    },\n" +
      "    \"underReview\": {\n" +
      "      \"en\": \"underReview\",\n" +
      "      \"fr\": \"underReview\"\n" +
      "    },\n" +
      "    \"approved\": {\n" +
      "      \"en\": \"approved\",\n" +
      "      \"fr\": \"approved\"\n" +
      "    },\n" +
      "    \"conditionallyApproved\": {\n" +
      "      \"en\": \"conditionallyApproved\",\n" +
      "      \"fr\": \"conditionallyApproved\"\n" +
      "    },\n" +
      "    \"rejected\": {\n" +
      "      \"en\": \"rejected\",\n" +
      "      \"fr\": \"rejected\"\n" +
      "    },\n" +
      "    \"detailedOverview\": {\n" +
      "      \"en\": \"detailedOverview\",\n" +
      "      \"fr\": \"detailedOverview\"\n" +
      "    },\n" +
      "    \"true\": {\n" +
      "      \"en\": \"YES\",\n" +
      "      \"fr\": \"OUI\"\n" +
      "    },\n" +
      "    \"false\": {\n" +
      "      \"en\": \"NO\",\n" +
      "      \"fr\": \"NON\"\n" +
      "    }\n" +
      "  },\n" +
      "  \"table\": {\n" +
      "    \"generic.accessRequestId\": {\n" +
      "      \"en\": \"ACCESS REQUEST ID\",\n" +
      "      \"fr\": \"ID DE LA REQUETE D'ACCES\"\n" +
      "    },\n" +
      "    \"generic.status\": {\n" +
      "      \"en\": \"STATUS\",\n" +
      "      \"fr\": \"STATUS\"\n" +
      "    },\n" +
      "    \"generic.creationDate\": {\n" +
      "      \"en\": \"CREATION DATE\",\n" +
      "      \"fr\": \"DATE DE CREATION\"\n" +
      "    },\n" +
      "    \"generic.firstSubmissionDate\": {\n" +
      "      \"en\": \"SUBMISSION DATE\",\n" +
      "      \"fr\": \"DATE DE SOUMISSION\"\n" +
      "    },\n" +
      "    \"generic.lastApprovedOrRejectedDate\": {\n" +
      "      \"en\": \"APPROVED / REJECTED DATE\",\n" +
      "      \"fr\": \"APPROVED / REJECTED DATE\"\n" +
      "    },\n" +
      "    \"generic.numberOfDaysBetweenFirstSubmissionAndApproveOrReject\": {\n" +
      "      \"en\": \"DAYS BETWEEN FIRST SUBMISSION DATE AND APPROVED/REJECTED DATE\",\n" +
      "      \"fr\": \"DAYS BETWEEN FIRST SUBMISSION DATE AND APPROVED/REJECTED DATE\"\n" +
      "    },\n" +
      "    \"title\": {\n" +
      "      \"en\": \"TITLE\",\n" +
      "      \"fr\": \"TITRE\"\n" +
      "    },\n" +
      "    \"isWaiting\": {\n" +
      "      \"en\": \"WAITING\",\n" +
      "      \"fr\": \"EN ATTENTE\"\n" +
      "    }\n" +
      "  }\n" +
      "}";
  }

  private String expectedResult() {
    return "\"<Organisation> Access Office\"\n" +
      "\"Rapport des demandes d'accès\"\n" +
      "\"" + new DateTime().toString(CsvReportGenerator.DATETIME_FORMAT) + "\"\n" +
      "\"\"\n" +
      "\"SOMMAIRE\"\n" +
      "\"STATUT ACTUEL\",\"NOMBRE DE DEMANDES D'ACCÈS\"\n" +
      "\"Ouvert\",\"1\"\n" +
      "\"submitted\",\"1\"\n" +
      "\"underReview\",\"0\"\n" +
      "\"approved\",\"1\"\n" +
      "\"conditionallyApproved\",\"0\"\n" +
      "\"rejected\",\"0\"\n" +
      "\"\"\n" +
      "\"detailedOverview\"\n" +
      "\"ID DE LA REQUETE D'ACCES\",\"STATUS\",\"DATE DE CREATION\",\"DATE DE SOUMISSION\",\"APPROVED / REJECTED DATE\",\"DAYS BETWEEN FIRST SUBMISSION DATE AND APPROVED/REJECTED DATE\",\"TITRE\",\"EN ATTENTE\"\n" +
      "\"refused id\",\"Ouvert\",\"20/08/2016\",\"01/06/2016\",\"N/A\",\"N/A\",\"Opened request title\",\"N/A\"\n"+
      "\"refused id\",\"submitted\",\"20/08/2016\",\"01/06/2016\",\"N/A\",\"N/A\",\"Opened request title\",\"OUI\"\n" +
      "\"approved id\",\"approved\",\"20/08/2016\",\"01/06/2016\",\"01/01/2017\",\"214\",\"Approved request title\",\"NON\"\n";
  }
}

