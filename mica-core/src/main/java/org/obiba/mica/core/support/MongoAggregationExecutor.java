package org.obiba.mica.core.support;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperationContext;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This utility class replaces the deprecated eval() for executing aggregation using JS scripts.
 */
public class MongoAggregationExecutor {

  final MongoTemplate mongoTemplate;

  public MongoAggregationExecutor(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  public static MongoAggregationExecutor newInstance(MongoTemplate mongoTemplate) {
    return new MongoAggregationExecutor(mongoTemplate);
  }

  /**
   * Executes the aggregation scripts on the input collection
   *
   * @param scripts JS aggregation scripts
   * @param collection Name of the collection the aggregation is executed on
   * @return DB results as a list of LinkedHashMap
   */
  public List<LinkedHashMap> execute(List<String> scripts, String collection) {
    Aggregation aggregation = Aggregation.newAggregation(
      scripts.stream().map(CustomAggregationOperation::new).collect(Collectors.toList())
    );

    AggregationResults<BasicDBObject> results = mongoTemplate.aggregate(aggregation, collection, BasicDBObject.class);
    return results.getMappedResults()
      .stream()
      .map(LinkedHashMap.class::cast)
      .collect(Collectors.toList());
  }

  /**
   * Utility class to create custom Mongo aggregation using JS scripts.
   */
  private static class CustomAggregationOperation implements AggregationOperation {

    private final String script;

    CustomAggregationOperation(String queryScript) {
      this.script = queryScript;
    }

    private DBObject parseScript() {
      // There are no parse() for SimpleDbList, use the same algorithm as the deprecated com.mongodb.util.JSON class
      if (script.charAt(0) == '[') {
        String arrScript = String.format("{stages:%s}", script);
        BasicDBObject parsed = BasicDBObject.parse(arrScript);
        return (BasicDBList)parsed.get("stages");
      }

      return BasicDBObject.parse(script);
    }

    @Override
    public DBObject toDBObject(AggregationOperationContext aggregationOperationContext) {
      return aggregationOperationContext.getMappedObject(parseScript());
    }
  }
}
