package org.obiba.mica.core.upgrade;

import com.google.common.eventbus.EventBus;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

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
      logger.info("Moving data access config out of data access form");
      DBObject dataAccessForm = mongoTemplate.execute(db -> db.getCollection("dataAccessForm").find().next());
      insertDataAccessConfig(dataAccessForm);
      logger.info("Updating and publishing data access form");
      updateAndPublishDataAccessForm(dataAccessForm);
      logger.info("Publishing data access feasibility form");
      DBObject dataAccessFeasibilityForm = mongoTemplate.execute(db -> db.getCollection("dataAccessFeasibilityForm").find().next());
      publishDataAccessFeasibilityForm(dataAccessFeasibilityForm);
      logger.info("Publishing data access amendment form");
      DBObject dataAccessAmendmentForm = mongoTemplate.execute(db -> db.getCollection("dataAccessAmendmentForm").find().next());
      publishDataAccessAmendmentForm(dataAccessAmendmentForm);

      logger.info("Applying form revision to submitted data access requests");
      DBCursor dataAccessRequests = mongoTemplate.execute(db -> db.collectionExists("dataAccessRequest") ? db.getCollection("dataAccessRequest").find() : null);
      applyDataAccessRequestFormRevision(dataAccessRequests);
      logger.info("Applying form revision to submitted data access feasibilities");
      DBCursor dataAccessFeasibilities = mongoTemplate.execute(db -> db.collectionExists("dataAccessFeasibility") ? db.getCollection("dataAccessFeasibility").find() : null);
      applyDataAccessFeasibilityFormRevision(dataAccessFeasibilities);
      logger.info("Applying form revision to submitted data access amendments");
      DBCursor dataAccessAmendments = mongoTemplate.execute(db -> db.collectionExists("dataAccessAmendment") ? db.getCollection("dataAccessAmendment").find() : null);
      applyDataAccessAmendmentFormRevision(dataAccessAmendments);
    } catch (RuntimeException e) {
      logger.error("Error occurred when trying to execute removeSetsVocabulariesTerms.", e);
    }
  }

  private void insertDataAccessConfig(DBObject dataAccessForm) {
    DBObject dataAccessConfig = new BasicDBObject();
    dataAccessConfig.putAll(dataAccessForm);
    dataAccessConfig.removeField("pdfTemplates");
    dataAccessConfig.removeField("pdfDownloadType");
    dataAccessConfig.removeField("csvExportFormat");
    dataAccessConfig.removeField("properties");
    dataAccessConfig.removeField("titleFieldPath");
    dataAccessConfig.removeField("summaryFieldPath");
    dataAccessConfig.removeField("endDateFieldPath");
    dataAccessConfig.removeField("schema");
    dataAccessConfig.removeField("definition");
    dataAccessConfig.removeField("_id");
    dataAccessConfig.put("version", 1);
    dataAccessConfig.put("_class", "org.obiba.mica.micaConfig.domain.DataAccessConfig");

    mongoTemplate.execute(db -> {
      if (!db.collectionExists("dataAccessConfig"))
        db.createCollection("dataAccessConfig", new BasicDBObject());
      return db.getCollection("dataAccessConfig").save(dataAccessConfig);
    });
  }

  private void updateAndPublishDataAccessForm(DBObject dataAccessForm) {
    // clean data access form from config info
    dataAccessForm.removeField("idLength");
    dataAccessForm.removeField("allowIdWithLeadingZeros");
    dataAccessForm.removeField("notifyCreated");
    dataAccessForm.removeField("notifySubmitted");
    dataAccessForm.removeField("notifyReviewed");
    dataAccessForm.removeField("notifyConditionallyApproved");
    dataAccessForm.removeField("notifyApproved");
    dataAccessForm.removeField("notifyRejected");
    dataAccessForm.removeField("notifyReopened");
    dataAccessForm.removeField("notifyCommented");
    dataAccessForm.removeField("notifyAttachment");
    dataAccessForm.removeField("notifyFinalReport");
    dataAccessForm.removeField("notifyIntermediateReport");
    dataAccessForm.removeField("createdSubject");
    dataAccessForm.removeField("submittedSubject");
    dataAccessForm.removeField("reviewedSubject");
    dataAccessForm.removeField("conditionallyApprovedSubject");
    dataAccessForm.removeField("approvedSubject");
    dataAccessForm.removeField("rejectedSubject");
    dataAccessForm.removeField("reopenedSubject");
    dataAccessForm.removeField("commentedSubject");
    dataAccessForm.removeField("attachmentSubject");
    dataAccessForm.removeField("finalReportSubject");
    dataAccessForm.removeField("intermediateReportSubject");
    dataAccessForm.removeField("withReview");
    dataAccessForm.removeField("withConditionalApproval");
    dataAccessForm.removeField("approvedFinal");
    dataAccessForm.removeField("rejectedFinal");
    dataAccessForm.removeField("predefinedActions");
    dataAccessForm.removeField("feasibilityEnabled");
    dataAccessForm.removeField("amendmentsEnabled");
    dataAccessForm.removeField("variablesEnabled");
    dataAccessForm.removeField("feasibilityVariablesEnabled");
    dataAccessForm.removeField("amendmentVariablesEnabled");
    dataAccessForm.removeField("daoCanEdit");
    dataAccessForm.removeField("nbOfDaysBeforeReport");
    dataAccessForm.put("lastModifiedDate", new Date());

    mongoTemplate.execute(db -> db.getCollection("dataAccessForm").save(dataAccessForm));

    // publish
    dataAccessForm.removeField("_id");
    dataAccessForm.put("revision", 1);

    mongoTemplate.execute(db -> db.getCollection("dataAccessForm").save(dataAccessForm));
  }

  private void publishDataAccessFeasibilityForm(DBObject dataAccessFeasibilityForm) {
    dataAccessFeasibilityForm.removeField("_id");
    dataAccessFeasibilityForm.put("revision", 1);

    mongoTemplate.execute(db -> db.getCollection("dataAccessFeasibilityForm").save(dataAccessFeasibilityForm));
  }

  private void publishDataAccessAmendmentForm(DBObject dataAccessAmendmentForm) {
    dataAccessAmendmentForm.removeField("_id");
    dataAccessAmendmentForm.put("revision", 1);

    mongoTemplate.execute(db -> db.getCollection("dataAccessAmendmentForm").save(dataAccessAmendmentForm));
  }

  private void applyDataAccessRequestFormRevision(DBCursor dataAccessRequests) {
    if (dataAccessRequests == null) return;
    while (dataAccessRequests.hasNext()) {
      DBObject dataAccessRequest = dataAccessRequests.next();
      if (!"OPENED".equals(dataAccessRequest.get("status").toString())) {
        dataAccessRequest.put("formRevision", 1);
        mongoTemplate.execute(db -> db.getCollection("dataAccessRequest").save(dataAccessRequest));
      }
    }
  }

  private void applyDataAccessFeasibilityFormRevision(DBCursor dataAccessFeasibilities) {
    if (dataAccessFeasibilities == null) return;
    while (dataAccessFeasibilities.hasNext()) {
      DBObject dataAccessRequest = dataAccessFeasibilities.next();
      if (!"OPENED".equals(dataAccessRequest.get("status").toString())) {
        dataAccessRequest.put("formRevision", 1);
        mongoTemplate.execute(db -> db.getCollection("dataAccessFeasibility").save(dataAccessRequest));
      }
    }
  }

  private void applyDataAccessAmendmentFormRevision(DBCursor dataAccessAmendments) {
    if (dataAccessAmendments == null) return;
    while (dataAccessAmendments.hasNext()) {
      DBObject dataAccessRequest = dataAccessAmendments.next();
      if (!"OPENED".equals(dataAccessRequest.get("status").toString())) {
        dataAccessRequest.put("formRevision", 1);
        mongoTemplate.execute(db -> db.getCollection("dataAccessAmendment").save(dataAccessRequest));
      }
    }
  }

}
