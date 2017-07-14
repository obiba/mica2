package org.obiba.mica.core.upgrade;

import org.obiba.mica.dataset.HarmonizationDatasetStateRepository;
import org.obiba.mica.dataset.service.HarmonizationDatasetService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class Mica3Upgrade implements UpgradeStep {

  private static final Logger logger = LoggerFactory.getLogger(Mica3Upgrade.class);

  @Inject
  private MongoTemplate mongoTemplate;

  @Inject
  private HarmonizationDatasetService harmonizationDatasetService;

  @Inject
  private HarmonizationDatasetStateRepository harmonizationDatasetStateRepository;

  @Override
  public String getDescription() {
    return "Mica 3.0.0 upgrade";
  }

  @Override
  public Version getAppliesTo() {
    return new Version(3, 0, 0);
  }

  @Override
  public void execute(Version version) {
    logger.info("Executing Mica upgrade to version 3.0.0");

    try {
      updateStudyResourcePathReferences();
    } catch (RuntimeException e) {
      logger.error("Error occurred when updating Study path resources (/study -> /individual-study and /study-dataset -> /collected-dataset).", e);
    }

    unpublishAllHarmonizationDataset();
  }

  private void unpublishAllHarmonizationDataset() {
    harmonizationDatasetStateRepository.findByPublishedTagNotNull().forEach(
      harmonizationDatasetState -> harmonizationDatasetService.unPublishState(harmonizationDatasetState.getId())
    );
  }

  private void updateStudyResourcePathReferences() {
    logger.info("Replacing all references to /study by /individual-study and /study-dataset by /collected-dataset and harmonization-dataset by harmonized-study...");
    mongoTemplate.execute(db -> db.eval(renameDocuments()));
  }

  private String renameDocuments() {
    return
      "function bulkUpdateAttachmentPath(collection, fields, regexp, replace) {\n" +
        "    var bulk = collection.initializeOrderedBulkOp();\n" +
        "    fields.forEach(function (field) {\n" +
        "        var findQuery = {};\n" +
        "        findQuery[field] = regexp;\n" +
        "        collection.find(findQuery).forEach(function (doc) {\n" +
        "            var replaceQuery = {};\n" +
        "            replaceQuery[field] = doc[field].replace(regexp, replace);\n" +
        "            bulk.find({\"_id\": doc._id}).updateOne({\"$set\": replaceQuery});\n" +
        "        });\n" +
        "    });\n" +
        "    bulk.execute();\n" +
        "};\n" +
        "bulkUpdateAttachmentPath(db.attachment, [\"path\"], /^\\/study-dataset\\//, '/collected-dataset/');\n" +
        "bulkUpdateAttachmentPath(db.attachmentState, [\"path\"], /^\\/study-dataset\\//, '/collected-dataset/');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/study-dataset$/, '/collected-dataset');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/study-dataset\\//, '/collected-dataset/');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/draft\\/study-dataset$/, '/draft/collected-dataset');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/draft\\/study-dataset\\//, '/draft/collected-dataset/');\n" +
        "\n" +
        "bulkUpdateAttachmentPath(db.attachment, [\"path\"], /^\\/harmonization-dataset\\//, '/harmonized-dataset/');\n" +
        "bulkUpdateAttachmentPath(db.attachmentState, [\"path\"], /^\\/harmonization-dataset\\//, '/harmonized-dataset/');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/harmonization-dataset$/, '/harmonized-dataset');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/harmonization-dataset\\//, '/harmonized-dataset/');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/draft\\/harmonization-dataset$/, '/draft/harmonized-dataset');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/draft\\/harmonization-dataset\\//, '/draft/harmonized-dataset/');\n" +
        "\n" +
        "bulkUpdateAttachmentPath(db.attachment, [\"path\"], /^\\/study\\//, '/individual-study/');\n" +
        "bulkUpdateAttachmentPath(db.attachmentState, [\"path\"], /^\\/study\\//, '/individual-study/');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/study$/, '/individual-study');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/study\\//, '/individual-study/');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/draft\\/study$/, '/draft/individual-study');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/draft\\/study\\//, '/draft/individual-study/');" +
        "";
  }
}
