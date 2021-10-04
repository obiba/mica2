package org.obiba.mica.core.upgrade;

import com.google.common.eventbus.EventBus;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

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
      updateDataAccessForm(dataAccessForm);
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
    dataAccessConfig.put("version", 1);
    dataAccessConfig.put("_class", "org.obiba.mica.micaConfig.domain.DataAccessConfig");

    mongoTemplate.execute(db -> {
      if (!db.collectionExists("dataAccessConfig"))
        db.createCollection("dataAccessConfig", new BasicDBObject());
      return db.getCollection("dataAccessConfig").save(dataAccessConfig);
    });
  }

  private void updateDataAccessForm(DBObject dataAccessForm) {
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

    mongoTemplate.execute(db -> db.getCollection("dataAccessForm").save(dataAccessForm));
  }

}
