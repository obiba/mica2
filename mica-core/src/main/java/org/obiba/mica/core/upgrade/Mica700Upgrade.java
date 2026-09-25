/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.upgrade;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import jakarta.inject.Inject;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.obiba.mica.file.FileUtils;
import org.obiba.runtime.Version;
import org.obiba.runtime.upgrade.UpgradeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * Fixes external editor ACLs (see {@code SubjectAclService}):
 * <ul>
 *   <li>ACLs created before the ADD-permission fix were saved as {@code VIEW,EDIT} and are shown as
 *       READER instead of EDITOR; add the missing ADD action.</li>
 *   <li>External editors were also given {@code _status} and {@code _attachments} child ACLs on the draft entity,
 *       which no permission check ever reads; remove them.</li>
 *   <li>ACLs of deleted projects, and child ACLs (e.g. comment) of entities deleted by someone other than the
 *       external editor, were never cleaned up; purge the orphans, including file ACLs.</li>
 * </ul>
 */
@Component
public class Mica700Upgrade implements UpgradeStep {

  private static final Logger logger = LoggerFactory.getLogger(Mica700Upgrade.class);

  private static final String SUBJECT_ACL_COLLECTION = "subjectAcl";

  // draft resource -> mongo collection holding the corresponding entity
  private static final Map<String, String> DRAFT_RESOURCE_TO_COLLECTION = new HashMap<String, String>() {
    {
      put("/draft/individual-study", "study");
      put("/draft/harmonization-study", "harmonizationStudy");
      put("/draft/network", "network");
      put("/draft/project", "project");
      put("/draft/collected-dataset", "studyDataset");
      put("/draft/harmonized-dataset", "harmonizationDataset");
    }
  };

  private static final List<String> FILE_RESOURCES = Arrays.asList("/file", "/draft/file");

  private final MongoTemplate mongoTemplate;

  @Inject
  public Mica700Upgrade(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public String getDescription() {
    return "Fix external editor ACLs and purge orphaned ACLs left by entity deletion";
  }

  @Override
  public Version getAppliesTo() {
    return new Version(7, 0, 0);
  }

  @Override
  public void execute(Version currentVersion) {
    logger.info("Executing Mica upgrade to version 7.0.0");
    // relies on the _status ACLs to identify external editors, so must run before they are removed
    upgradeExternalEditorAcls();
    removeUnusedExternalEditorChildAcls();
    purgeOrphanAcls();
  }

  private void upgradeExternalEditorAcls() {
    MongoCollection<Document> acls = mongoTemplate.getCollection(SUBJECT_ACL_COLLECTION);

    for (String draftResource : DRAFT_RESOURCE_TO_COLLECTION.keySet()) {
      Document query = new Document("resource", draftResource)
        .append("type", "USER")
        .append("actions", new Document("$all", Arrays.asList("VIEW", "EDIT")).append("$size", 2));

      try (MongoCursor<Document> cursor = acls.find(query).cursor()) {
        while (cursor.hasNext()) {
          Document acl = cursor.next();
          String principal = acl.getString("principal");
          String instance = acl.getString("instance");

          boolean hasStatusAcl = acls.find(new Document("resource", draftResource + "/" + instance)
            .append("instance", "_status")
            .append("principal", principal)
            .append("type", "USER")).first() != null;

          if (hasStatusAcl) {
            logger.info("Adding missing ADD action to external editor ACL of '{}' on '{}/{}'", principal,
              draftResource, instance);
            acls.updateOne(new Document("_id", acl.get("_id")),
              new Document("$set", new Document("actions", Arrays.asList("VIEW", "EDIT", "ADD"))));
          }
        }
      }
    }
  }

  private void removeUnusedExternalEditorChildAcls() {
    MongoCollection<Document> acls = mongoTemplate.getCollection(SUBJECT_ACL_COLLECTION);

    for (String draftResource : DRAFT_RESOURCE_TO_COLLECTION.keySet()) {
      String quoted = Pattern.quote(draftResource);
      long count = acls.deleteMany(new Document("resource", new Document("$regex", "^" + quoted + "/[^/]+$"))
        .append("instance", "_status")).getDeletedCount();
      count += acls.deleteMany(new Document("resource", new Document("$regex", "^" + quoted + "/[^/]+/_attachments$")))
        .getDeletedCount();
      if (count > 0) logger.info("Removed {} unused _status/_attachments ACLs on '{}'", count, draftResource);
    }
  }

  private void purgeOrphanAcls() {
    MongoCollection<Document> acls = mongoTemplate.getCollection(SUBJECT_ACL_COLLECTION);

    for (Map.Entry<String, String> entry : DRAFT_RESOURCE_TO_COLLECTION.entrySet()) {
      String draftResource = entry.getKey();
      String resource = draftResource.substring("/draft".length());
      Set<String> existingIds = findExistingIds(entry.getValue());

      purgeOrphansOnResource(acls, resource, existingIds);
      purgeOrphansOnResource(acls, draftResource, existingIds);
      purgeOrphanChildrenOnResource(acls, draftResource, existingIds);
      for (String fileResource : FILE_RESOURCES) {
        purgeOrphanFileAcls(acls, fileResource, resource, existingIds);
      }
    }
  }

  /**
   * Ids of the existing entities, both raw and encoded: entity ACL instances are encoded
   * ({@code SubjectAclService#encode}) while child resource paths are built from the raw id.
   */
  private Set<String> findExistingIds(String collectionName) {
    Set<String> ids = new HashSet<>();
    try (MongoCursor<Document> cursor = mongoTemplate.getCollection(collectionName).find()
      .projection(new Document("_id", 1)).cursor()) {
      while (cursor.hasNext()) {
        String id = cursor.next().getString("_id");
        ids.add(id);
        ids.add(FileUtils.encode(id));
      }
    }
    return ids;
  }

  private boolean isOrphan(String id, Set<String> existingIds) {
    return id != null && !id.isEmpty() && !"*".equals(id) && !existingIds.contains(id);
  }

  private void purgeOrphansOnResource(MongoCollection<Document> acls, String resource, Set<String> existingIds) {
    try (MongoCursor<Document> cursor = acls.find(new Document("resource", resource)).cursor()) {
      while (cursor.hasNext()) {
        Document acl = cursor.next();
        String instance = acl.getString("instance");
        if (isOrphan(instance, existingIds)) {
          logger.info("Purging orphan ACL on resource '{}' instance '{}'", resource, instance);
          acls.deleteOne(new Document("_id", acl.get("_id")));
        }
      }
    }
  }

  /**
   * Purges ACLs whose resource is a child of the draft entity itself, i.e. exactly
   * "{@code <draftResource>/<id>}" or starting with "{@code <draftResource>/<id>/}" (comment, ...), for an id that
   * no longer exists.
   */
  private void purgeOrphanChildrenOnResource(MongoCollection<Document> acls, String draftResource,
                                             Set<String> existingIds) {
    String prefix = draftResource + "/";
    Document query = new Document("resource", new Document("$regex", "^" + Pattern.quote(prefix)));

    try (MongoCursor<Document> cursor = acls.find(query).cursor()) {
      while (cursor.hasNext()) {
        Document acl = cursor.next();
        String childResource = acl.getString("resource");
        if (isOrphan(firstSegment(childResource.substring(prefix.length())), existingIds)) {
          logger.info("Purging orphan child ACL on resource '{}'", childResource);
          acls.deleteOne(new Document("_id", acl.get("_id")));
        }
      }
    }
  }

  /**
   * Purges file ACLs whose instance is the entity's folder "{@code <resource>/<id>}" or one of its descendants,
   * for an id that no longer exists.
   */
  private void purgeOrphanFileAcls(MongoCollection<Document> acls, String fileResource, String resource,
                                   Set<String> existingIds) {
    String prefix = resource + "/";
    Document query = new Document("resource", fileResource)
      .append("instance", new Document("$regex", "^" + Pattern.quote(prefix)));

    try (MongoCursor<Document> cursor = acls.find(query).cursor()) {
      while (cursor.hasNext()) {
        Document acl = cursor.next();
        String instance = acl.getString("instance");
        if (isOrphan(firstSegment(instance.substring(prefix.length())), existingIds)) {
          logger.info("Purging orphan file ACL on resource '{}' instance '{}'", fileResource, instance);
          acls.deleteOne(new Document("_id", acl.get("_id")));
        }
      }
    }
  }

  private String firstSegment(String path) {
    int slash = path.indexOf('/');
    return slash < 0 ? path : path.substring(0, slash);
  }

}
