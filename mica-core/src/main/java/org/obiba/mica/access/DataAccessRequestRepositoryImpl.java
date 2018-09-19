/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.core.repository.AttachmentAwareRepository;
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.file.FileStoreService;
import org.slf4j.Logger;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DataAccessRequestRepositoryImpl
  implements DataAccessRequestRepositoryCustom, AttachmentAwareRepository<DataAccessRequest> {

  private static final Logger logger = org.slf4j.LoggerFactory.getLogger(DataAccessRequestRepositoryImpl.class);

  @Inject
  AttachmentRepository attachmentRepository;

  @Inject
  FileStoreService fileStoreService;

  @Inject
  MongoTemplate mongoTemplate;

  @Override
  public AttachmentRepository getAttachmentRepository() {
    return attachmentRepository;
  }

  @Override
  public FileStoreService getFileStoreService() {
    return fileStoreService;
  }

  @Override
  public String getAttachmentPath(DataAccessRequest dataAccessRequest) {
    return String.format("/data-access-request/%s", dataAccessRequest.getId());
  }

  @Override
  public DataAccessRequest saveWithReferences(DataAccessRequest dataAccessRequest) {
    saveAttachments(dataAccessRequest);
    mongoTemplate.save(dataAccessRequest);

    return dataAccessRequest;
  }

  @Override
  public void deleteWithReferences(DataAccessRequest dataAccessRequest) {
    mongoTemplate.remove(dataAccessRequest);
    deleteAttachments(dataAccessRequest);
  }

  @Override
  public Map<Object, LinkedHashMap> getAllAmendmentsSummary() {
    return getAmendmentsSummaryInternal(null);
  }

  @Override
  public Map<Object, LinkedHashMap> getAmendmentsSummary(String id) {
    return getAmendmentsSummaryInternal(id);
  }

  private Map<Object, LinkedHashMap> getAmendmentsSummaryInternal(String id) {
    String match = Strings.isNullOrEmpty(id) ? "" : "{$match: {\"parentId\": \"" + id + "\"}},";
    String group =
      "{\n" +
      "  $group: {" +
      "    _id: \"$parentId\"," +
      "    pending: {" +
      "      $sum: {" +
      "        $cond: { " +
      "           if: { $or: [ { $eq: [ \"$status\", \"APPROVED\" ] }, { $eq: [ \"$status\", \"REJECTED\" ] } ] }, " +
      "           then: 0, "+
      "           else: 1" +
      "        }" +
      "      }" +
      "    }," +
      "    total: {" +
      "      $sum: 1" +
      "    }" +
      "  }" +
      "}";

    String aggregate = String.format("db.dataAccessAmendment.aggregate([%s%s])", match, group);

    try {
      BasicDBObject result = (BasicDBObject) mongoTemplate.execute(db -> db.eval(aggregate));
      return result.values().stream()
        .filter(BasicDBList.class::isInstance)
        .map(BasicDBList.class::cast)
        .flatMap(Collection::stream)
        .map(LinkedHashMap.class::cast)
        .collect(Collectors.toMap(entry -> entry.get("_id"), entry -> entry));

    } catch (RuntimeException e) {
      logger.error("Error occurred executing db.dataAccessAmendment.aggregate ().", e);
      return Maps.newLinkedHashMap();
    }
  }
}
