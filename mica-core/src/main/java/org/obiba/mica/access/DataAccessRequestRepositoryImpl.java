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
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.core.repository.AttachmentAwareRepository;
import org.obiba.mica.core.repository.AttachmentRepository;
import org.obiba.mica.core.support.MongoAggregationExecutor;
import org.obiba.mica.file.FileStoreService;
import org.slf4j.Logger;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DataAccessRequestRepositoryImpl
  implements DataAccessRequestRepositoryCustom, AttachmentAwareRepository<DataAccessRequest> {

  private static final Logger logger = org.slf4j.LoggerFactory.getLogger(DataAccessRequestRepositoryImpl.class);

  final AttachmentRepository attachmentRepository;

  final
  FileStoreService fileStoreService;

  final
  MongoTemplate mongoTemplate;

  private final MongoAggregationExecutor aggrgationExecutor;

  @Inject
  public DataAccessRequestRepositoryImpl(AttachmentRepository attachmentRepository,
                                         FileStoreService fileStoreService,
                                         MongoTemplate mongoTemplate) {
    this.attachmentRepository = attachmentRepository;
    this.fileStoreService = fileStoreService;
    this.mongoTemplate = mongoTemplate;
    this.aggrgationExecutor = MongoAggregationExecutor.newInstance(mongoTemplate);
  }

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

    List<String> aggScripts = Arrays.asList(
      "{$sort: {lastModifiedDate: 1}}",
      "    {\n" +
      "      $group: {\n" +
      "      _id: \"$parentId\",\n" +
      "        pending: {\n" +
      "          $sum: {\n" +
      "            $cond: {\n" +
      "              if: { $or: [ { $eq: [ \"$status\", \"APPROVED\" ] }, { $eq: [ \"$status\", \"REJECTED\" ] } ] }, \n" +
      "              then: 0, \n" +
      "              else: 1\n" +
      "            }\n" +
      "          }\n" +
      "        },\n" +
      "        lastModified: { \n" +
      "          $last: \"$lastModifiedDate\"\n" +
      "        },\n" +
      "        total: {\n" +
      "          $sum: 1\n" +
      "        }\n" +
      "      }\n" +
      "    }"
    );

    if (!Strings.isNullOrEmpty(id)) {
      aggScripts.add(0, "{$match: {\"parentId\": \"" + id + "\"}}");
    }

    try {
      List<LinkedHashMap> results = aggrgationExecutor.execute(aggScripts, "dataAccessAmendment");
      return results.stream().collect(Collectors.toMap(entry -> entry.get("_id"), entry -> entry));


    } catch (RuntimeException e) {
      logger.error("Error occurred executing db.dataAccessAmendment.aggregate ().", e);
      return Maps.newLinkedHashMap();
    }

  }

  public List<LinkedHashMap> getCountByStatus() {
    String aggOperation = "" +
      "{" +
      "  $group : {_id: '$status', count: {$sum : 1}}},\n" +
      "  { $project: { _id: 0 } " +
      "}";
    return aggrgationExecutor.execute(Arrays.asList(aggOperation), "dataAccessRequest");
  }
}
