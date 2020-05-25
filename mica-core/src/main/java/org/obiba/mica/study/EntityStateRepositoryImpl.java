package org.obiba.mica.study;

import com.google.common.base.CaseFormat;
import org.obiba.mica.core.support.MongoAggregationExecutor;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Provides a count for each entity state status as well as the total document count.
 *
 * -
 * - total
 */
public class EntityStateRepositoryImpl implements EntityStateRepositoryCustom {

  final protected MongoTemplate mongoTemplate;

  private final MongoAggregationExecutor aggregationExecutor;

  private final String collection;

  public EntityStateRepositoryImpl(MongoTemplate mongoTemplate, String collection) {
    this.mongoTemplate = mongoTemplate;
    this.aggregationExecutor = MongoAggregationExecutor.newInstance(mongoTemplate);
    this.collection = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, collection);
  }

  @Override
  public List<LinkedHashMap> countByEachStateStatus() {
    List<String> aggOperations = Arrays.asList(
      "{\n" +
        "    \"$group\": {\n" +
        "      \"_id\": \"" + collection + "\",\n" +
        "      \"total\": {\n" +
        "        \"$sum\": 1\n" +
        "      },\n" +
        "      \"published\": {\n" +
        "        \"$sum\": { \"$cond\": [ { \"$ifNull\": [ \"$publishedTag\", false ] }, 1, 0 ] }\n" +
        "      },     \n" +
        "      \"reviewing\": {\n" +
        "        \"$sum\": { \"$cond\": [ { \"$eq\": [ \"UNDER_REVIEW\", \"$revisionStatus\" ] }, 1, 0 ] }\n" +
        "      },\n" +
        "      \"deleting\": {\n" +
        "        \"$sum\": { \"$cond\": [ { \"$eq\": [ \"DELETED\", \"$revisionStatus\" ] }, 1, 0 ] }\n" +
        "      },\n" +
        "      \"editing\": {\n" +
        "        \"$sum\": {\"$cond\": [{\"$and\": [{\"$gt\": [\"$revisionsAhead\", 0 ] }, {\"$ifNull\": [\"$publishedTag\", false ] } ] }, 1, 0 ] }\n" +
        "      }\n" +
        "    }\n" +
        "  }"
    );

    return aggregationExecutor.execute(aggOperations, collection);
  }
}
