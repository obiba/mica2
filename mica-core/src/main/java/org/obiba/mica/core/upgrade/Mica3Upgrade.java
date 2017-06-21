package org.obiba.mica.core.upgrade;

import org.obiba.mica.core.service.GitService;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class Mica3Upgrade implements UpgradeStep {

  private static final Logger logger = LoggerFactory.getLogger(Mica3Upgrade.class);

  @Inject
  private MongoTemplate mongoTemplate;

  @Inject
  private GitService gitService;

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
      moveGitRepositoryOfStudies();
    } catch (IOException e) {
      logger.error("Error occurred when moving git repository from studies to collectionStudies.", e);
    }

    try {
      updateStudyResourcePathReferences();
    } catch (RuntimeException e) {
      logger.error("Error occurred when updating Study path resources (/study -> /collection-study).", e);
    }
  }

  private void moveGitRepositoryOfStudies() throws IOException {
    File repositoriesRoot = gitService.getRepositoriesRoot();
    Path oldGitPath = new File(repositoriesRoot, "/studies").toPath();
    Path newGitPath = new File(repositoriesRoot, "/collectionStudies").toPath();
    Files.move(oldGitPath, newGitPath);
  }

  private void updateStudyResourcePathReferences() {
    logger.info("Replacing all references to /study by /collection-study...");
    mongoTemplate.execute(db -> db.eval(replaceStudyByCollectionStudy()));
  }

  public String replaceStudyByCollectionStudy() {
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
        "bulkUpdateAttachmentPath(db.attachment, [\"path\"], /^\\/study-dataset\\//, '/collection-dataset/');\n" +
        "bulkUpdateAttachmentPath(db.attachmentState, [\"path\"], /^\\/study-dataset\\//, '/collection-dataset/');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/study-dataset$/, '/collection-dataset');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/study-dataset\\//, '/collection-dataset/');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/draft\\/study-dataset$/, '/draft/collection-dataset');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/draft\\/study-dataset\\//, '/draft/collection-dataset/');\n" +
        "\n" +
        "bulkUpdateAttachmentPath(db.attachment, [\"path\"], /^\\/study\\//, '/collection-study/');\n" +
        "bulkUpdateAttachmentPath(db.attachmentState, [\"path\"], /^\\/study\\//, '/collection-study/');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/study$/, '/collection-study');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/study\\//, '/collection-study/');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/draft\\/study$/, '/draft/collection-study');\n" +
        "bulkUpdateAttachmentPath(db.subjectAcl, [\"resource\", \"instance\"], /^\\/draft\\/study\\//, '/draft/collection-study/');" +
        "";
  }
}
