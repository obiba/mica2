package org.obiba.mica.core.upgrade;

import com.google.common.eventbus.EventBus;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;

import org.bson.Document;
import org.obiba.mica.micaConfig.event.TaxonomiesUpdatedEvent;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;

@Component
public class Mica460Upgrade implements UpgradeStep {

  private static final Logger logger = LoggerFactory.getLogger(Mica460Upgrade.class);

  private MongoTemplate mongoTemplate;

  private EventBus eventBus;

  public Mica460Upgrade(
    MongoTemplate mongoTemplate,
    EventBus eventBus) {
    this.mongoTemplate = mongoTemplate;
    this.eventBus = eventBus;
  }

  @Override
  public String getDescription() {
    return "Upgrade data to 4.6.0";
  }

  @Override
  public Version getAppliesTo() {
    return new Version(4, 6, 0);
  }

  @Override
  public void execute(Version currentVersion) {
    logger.info("Executing Mica upgrade to version 4.6.0");
    try {
      Document dataAccessForm = mongoTemplate.getCollection("dataAccessForm").find().first();
      Document dataAccessFeasibilityForm = mongoTemplate.getCollection("dataAccessFeasibilityForm").find().first();
      Document dataAccessAmendmentForm = mongoTemplate.getCollection("dataAccessAmendmentForm").find().first();

      String csvExportFormat = dataAccessForm.get("csvExportFormat").toString();
      String feasibilityCsvExportFormat = dataAccessFeasibilityForm.get("csvExportFormat").toString();
      String amendmentCsvExportFormat = dataAccessAmendmentForm.get("csvExportFormat").toString();

      logger.info("Moving data access config out of data access form");
      insertDataAccessConfig(dataAccessForm, csvExportFormat, feasibilityCsvExportFormat, amendmentCsvExportFormat);
      logger.info("Updating and publishing data access form");
      updateAndPublishDataAccessForm(dataAccessForm);
      logger.info("Updating and publishing data access feasibility form");
      updateAndPublishDataAccessFeasibilityForm(dataAccessFeasibilityForm);
      logger.info("Updating and publishing data access amendment form");
      updateAndPublishDataAccessAmendmentForm(dataAccessAmendmentForm);

      logger.info("Applying form revision to submitted data access requests");
      MongoCursor<Document> dataAccessRequests = mongoTemplate.getCollection("dataAccessRequest").find().cursor();
      applyDataAccessRequestFormRevision(dataAccessRequests);
      logger.info("Applying form revision to submitted data access feasibilities");
      MongoCursor<Document> dataAccessFeasibilities = mongoTemplate.getCollection("dataAccessFeasibility").find().cursor();
      applyDataAccessFeasibilityFormRevision(dataAccessFeasibilities);
      logger.info("Applying form revision to submitted data access amendments");
      MongoCursor<Document> dataAccessAmendments = mongoTemplate.getCollection("dataAccessAmendment").find().cursor();
      applyDataAccessAmendmentFormRevision(dataAccessAmendments);
    } catch (RuntimeException e) {
      logger.error("Error occurred when trying to update data access forms or requests.", e);
    }

    try {
      logger.info("Adding 'sets' vocabulary to the network taxonomy");
      Document networkTaxonomy = mongoTemplate.getCollection("taxonomyEntityWrapper").find(Filters.eq("network")).first();
      if (networkTaxonomy != null) {
        DBObject setsVocabulary = BasicDBObject.parse("{\n" +
          "  \"name\" : \"sets\",\n" +
          "  \"title\" : {\n" +
          "      \"en\" : \"Sets\",\n" +
          "      \"fr\" : \"Ensembles\"\n" +
          "  },\n" +
          "  \"description\" : {\n" +
          "      \"en\" : \"Sets in which the network appears.\",\n" +
          "      \"fr\" : \"Ensembles dans lesquels est définie le réseau.\"\n" +
          "  },\n" +
          "  \"repeatable\" : false,\n" +
          "  \"keywords\" : {},\n" +
          "  \"attributes\" : {\n" +
          "      \"static\" : \"true\",\n" +
          "      \"hidden\" : \"true\",\n" +
          "      \"type\" : \"keyword\"\n" +
          "  }\n" +
          "}");
        ((ArrayList<Object>)(((DBObject) networkTaxonomy.get("taxonomy")).get("vocabularies"))).add(setsVocabulary);
        mongoTemplate.save(networkTaxonomy, "taxonomyEntityWrapper");
      }

      logger.info("Adding 'sets' vocabulary to the study taxonomy");
      Document studyTaxonomy = mongoTemplate.getCollection("taxonomyEntityWrapper").find(Filters.eq("study")).first();
      if (studyTaxonomy != null) {
        DBObject setsVocabulary = BasicDBObject.parse("{\n" +
          "  \"name\" : \"sets\",\n" +
          "  \"title\" : {\n" +
          "      \"en\" : \"Sets\",\n" +
          "      \"fr\" : \"Ensembles\"\n" +
          "  },\n" +
          "  \"description\" : {\n" +
          "      \"en\" : \"Sets in which the study appears.\",\n" +
          "      \"fr\" : \"Ensembles dans lesquels est définie l'étude.\"\n" +
          "  },\n" +
          "  \"repeatable\" : false,\n" +
          "  \"keywords\" : {},\n" +
          "  \"attributes\" : {\n" +
          "      \"static\" : \"true\",\n" +
          "      \"hidden\" : \"true\",\n" +
          "      \"type\" : \"keyword\"\n" +
          "  }\n" +
          "}");
        ((ArrayList<Object>)(((DBObject) studyTaxonomy.get("taxonomy")).get("vocabularies"))).add(setsVocabulary);
        mongoTemplate.save(studyTaxonomy, "taxonomyEntityWrapper");
      }

      logger.info("Indexing Taxonomies");
      eventBus.post(new TaxonomiesUpdatedEvent());
    } catch(Exception e) {
      logger.error("Failed to index Taxonomies", e);
    }
  }

  private void insertDataAccessConfig(Document dataAccessForm, String csvExportFormat, String feasibilityCsvExportFormat, String amendmentCsvExportFormat) {
    Document dataAccessConfig = new Document();
    dataAccessConfig.putAll(dataAccessForm);
    dataAccessConfig.put("csvExportFormat", csvExportFormat);
    dataAccessConfig.put("feasibilityCsvExportFormat", feasibilityCsvExportFormat);
    dataAccessConfig.put("amendmentCsvExportFormat", amendmentCsvExportFormat);
    dataAccessConfig.remove("pdfTemplates");
    dataAccessConfig.remove("pdfDownloadType");
    dataAccessConfig.remove("properties");
    dataAccessConfig.remove("titleFieldPath");
    dataAccessConfig.remove("summaryFieldPath");
    dataAccessConfig.remove("endDateFieldPath");
    dataAccessConfig.remove("schema");
    dataAccessConfig.remove("definition");
    dataAccessConfig.remove("_id");
    dataAccessConfig.put("version", 1);
    dataAccessConfig.put("_class", "org.obiba.mica.micaConfig.domain.DataAccessConfig");

    mongoTemplate.getCollection("dataAccessConfig").insertOne(dataAccessConfig);
  }

  private void updateAndPublishDataAccessForm(Document dataAccessForm) {
    // clean data access form from config info
    dataAccessForm.remove("idLength");
    dataAccessForm.remove("allowIdWithLeadingZeros");
    dataAccessForm.remove("notifyCreated");
    dataAccessForm.remove("notifySubmitted");
    dataAccessForm.remove("notifyReviewed");
    dataAccessForm.remove("notifyConditionallyApproved");
    dataAccessForm.remove("notifyApproved");
    dataAccessForm.remove("notifyRejected");
    dataAccessForm.remove("notifyReopened");
    dataAccessForm.remove("notifyCommented");
    dataAccessForm.remove("notifyAttachment");
    dataAccessForm.remove("notifyFinalReport");
    dataAccessForm.remove("notifyIntermediateReport");
    dataAccessForm.remove("createdSubject");
    dataAccessForm.remove("submittedSubject");
    dataAccessForm.remove("reviewedSubject");
    dataAccessForm.remove("conditionallyApprovedSubject");
    dataAccessForm.remove("approvedSubject");
    dataAccessForm.remove("rejectedSubject");
    dataAccessForm.remove("reopenedSubject");
    dataAccessForm.remove("commentedSubject");
    dataAccessForm.remove("attachmentSubject");
    dataAccessForm.remove("finalReportSubject");
    dataAccessForm.remove("intermediateReportSubject");
    dataAccessForm.remove("withReview");
    dataAccessForm.remove("withConditionalApproval");
    dataAccessForm.remove("approvedFinal");
    dataAccessForm.remove("rejectedFinal");
    dataAccessForm.remove("predefinedActions");
    dataAccessForm.remove("feasibilityEnabled");
    dataAccessForm.remove("amendmentsEnabled");
    dataAccessForm.remove("variablesEnabled");
    dataAccessForm.remove("feasibilityVariablesEnabled");
    dataAccessForm.remove("amendmentVariablesEnabled");
    dataAccessForm.remove("daoCanEdit");
    dataAccessForm.remove("nbOfDaysBeforeReport");
    dataAccessForm.remove("csvExportFormat");
    dataAccessForm.put("lastUpdateDate", new Date());
    mongoTemplate.save(dataAccessForm, "dataAccessForm");

    // publish
    dataAccessForm.remove("_id");
    dataAccessForm.put("revision", 1);
    mongoTemplate.save(dataAccessForm, "dataAccessForm");
  }

  private void updateAndPublishDataAccessFeasibilityForm(Document dataAccessFeasibilityForm) {
    // update
    dataAccessFeasibilityForm.remove("csvExportFormat");
    dataAccessFeasibilityForm.put("lastUpdateDate", new Date());
    mongoTemplate.save(dataAccessFeasibilityForm, "dataAccessFeasibilityForm");

    // publish
    dataAccessFeasibilityForm.remove("_id");
    dataAccessFeasibilityForm.put("revision", 1);
    mongoTemplate.save(dataAccessFeasibilityForm, "dataAccessFeasibilityForm");
  }

  private void updateAndPublishDataAccessAmendmentForm(Document dataAccessAmendmentForm) {
    // update
    dataAccessAmendmentForm.remove("csvExportFormat");
    dataAccessAmendmentForm.put("lastUpdateDate", new Date());
    mongoTemplate.save(dataAccessAmendmentForm, "dataAccessAmendmentForm");

    // publish
    dataAccessAmendmentForm.remove("_id");
    dataAccessAmendmentForm.put("revision", 1);
    mongoTemplate.save(dataAccessAmendmentForm, "dataAccessAmendmentForm");
  }

  private void applyDataAccessRequestFormRevision(MongoCursor<Document> dataAccessRequests) {
    if (dataAccessRequests == null) return;
    while (dataAccessRequests.hasNext()) {
      Document dataAccessRequest = dataAccessRequests.next();
      if (!"OPENED".equals(dataAccessRequest.get("status").toString())) {
        dataAccessRequest.put("formRevision", 1);
        mongoTemplate.save(dataAccessRequest, "dataAccessRequest");
      }
    }
  }

  private void applyDataAccessFeasibilityFormRevision(MongoCursor<Document> dataAccessFeasibilities) {
    if (dataAccessFeasibilities == null) return;
    while (dataAccessFeasibilities.hasNext()) {
      Document dataAccessRequest = dataAccessFeasibilities.next();
      if (!"OPENED".equals(dataAccessRequest.get("status").toString())) {
        dataAccessRequest.put("formRevision", 1);
        mongoTemplate.save(dataAccessRequest, "dataAccessFeasibility");
      }
    }
  }

  private void applyDataAccessAmendmentFormRevision(MongoCursor<Document> dataAccessAmendments) {
    if (dataAccessAmendments == null) return;
    while (dataAccessAmendments.hasNext()) {
      Document dataAccessRequest = dataAccessAmendments.next();
      if (!"OPENED".equals(dataAccessRequest.get("status").toString())) {
        dataAccessRequest.put("formRevision", 1);
        mongoTemplate.save(dataAccessRequest, "dataAccessAmendment");
      }
    }
  }

}
