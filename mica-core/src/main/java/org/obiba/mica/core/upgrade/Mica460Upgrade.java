package org.obiba.mica.core.upgrade;

import com.google.common.eventbus.EventBus;
import org.obiba.mica.micaConfig.event.TaxonomiesUpdatedEvent;
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
      mongoTemplate.execute(db -> db.eval(initDataAccessConfig()));
      mongoTemplate.execute(db -> db.eval(cleanDataAccessForm()));
    } catch (RuntimeException e) {
      logger.error("Error occurred when trying to execute removeSetsVocabulariesTerms.", e);
    }
  }

  private String initDataAccessConfig() {
    return "db.getCollection('dataAccessForm').find({})\n" +
      ".map(function(doc) {\n" +
      "    delete doc.pdfTemplates;\n" +
      "    delete doc.pdfDownloadType;\n" +
      "    delete doc.csvExportFormat;\n" +
      "    delete doc.properties;\n" +
      "    delete doc.titleFieldPath;\n" +
      "    delete doc.summaryFieldPath;\n" +
      "    delete doc.endDateFieldPath;\n" +
      "    delete doc.schema;\n" +
      "    delete doc.definition;\n" +
      "    doc.version = 1;\n" +
      "    doc._class = 'org.obiba.mica.micaConfig.domain.DataAccessConfig';\n" +
      "    doc._id = new ObjectId();\n" +
      "    return doc;\n" +
      "    })\n" +
      ".forEach(function(doc) { \n" +
      "    db.getCollection('dataAccessConfig').save(doc);\n" +
      "    })";
  }

  private String cleanDataAccessForm() {
    return "db.getCollection('dataAccessForm').find({})\n" +
      ".map(function(doc) {\n" +
      "    delete doc.idLength;\n" +
      "    delete doc.allowIdWithLeadingZeros;\n" +
      "    delete doc.notifyCreated;\n" +
      "    delete doc.notifySubmitted;\n" +
      "    delete doc.notifyReviewed;\n" +
      "    delete doc.notifyConditionallyApproved;\n" +
      "    delete doc.notifyApproved;\n" +
      "    delete doc.notifyRejected;\n" +
      "    delete doc.notifyReopened;\n" +
      "    delete doc.notifyCommented;\n" +
      "    delete doc.notifyAttachment;\n" +
      "    delete doc.notifyFinalReport;\n" +
      "    delete doc.notifyIntermediateReport;\n" +
      "    delete doc.createdSubject;\n" +
      "    delete doc.submittedSubject;\n" +
      "    delete doc.reviewedSubject;\n" +
      "    delete doc.conditionallyApprovedSubject;\n" +
      "    delete doc.approvedSubject;\n" +
      "    delete doc.rejectedSubject;\n" +
      "    delete doc.reopenedSubject;\n" +
      "    delete doc.commentedSubject;\n" +
      "    delete doc.attachmentSubject;\n" +
      "    delete doc.finalReportSubject;\n" +
      "    delete doc.intermediateReportSubject;\n" +
      "    delete doc.withReview;\n" +
      "    delete doc.withConditionalApproval;\n" +
      "    delete doc.approvedFinal;\n" +
      "    delete doc.rejectedFinal;\n" +
      "    delete doc.predefinedActions;\n" +
      "    delete doc.feasibilityEnabled;\n" +
      "    delete doc.amendmentsEnabled;\n" +
      "    delete doc.variablesEnabled;\n" +
      "    delete doc.feasibilityVariablesEnabled;\n" +
      "    delete doc.amendmentVariablesEnabled;\n" +
      "    delete doc.daoCanEdit;\n" +
      "    delete doc.nbOfDaysBeforeReport;\n" +
      "    return doc;\n" +
      "    })\n" +
      ".forEach(function(doc) { \n" +
      "    db.getCollection('dataAccessForm').save(doc);\n" +
      "    })";
  }
}
