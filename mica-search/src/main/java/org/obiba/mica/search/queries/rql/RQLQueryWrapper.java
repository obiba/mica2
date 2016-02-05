/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.queries.rql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.jazdw.rql.parser.ASTNode;
import net.jazdw.rql.parser.RQLParser;
import net.jazdw.rql.parser.SimpleASTVisitor;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.obiba.mica.core.domain.AttributeKey;
import org.obiba.mica.search.queries.QueryWrapper;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 *
 */
public class RQLQueryWrapper implements QueryWrapper {

  private int from = DEFAULT_FROM;

  private int size = DEFAULT_SIZE;

  private ASTNode node;

  private QueryBuilder queryBuilder;

  private SortBuilder sortBuilder;

  private List<String> aggregations;

  private List<String> aggregationBuckets;

  private final Map<String, Map<String, List<String>>> taxonomyTermsMap = Maps.newHashMap();

  public RQLQueryWrapper(String rql) {
    this(new RQLParser().parse(rql));
  }

  public RQLQueryWrapper(ASTNode node) {
    try {
      RQLNode type = RQLNode.getType(node.getName());
      switch(type) {
        case VARIABLE:
        case DATASET:
        case STUDY:
        case NETWORK:
          node.getArguments().stream().map(a -> (ASTNode) a).forEach(n -> {
            if(n.getName().equals(RQLNode.LIMIT.name().toLowerCase())) parseLimit(n);
            else if(n.getName().equals(RQLNode.SORT.name().toLowerCase())) parseSort(n);
            else if(n.getName().equals(RQLNode.AGGREGATE.name().toLowerCase())) parseAggregate(n);
            else parseQuery(n);
          });
          break;
        default:
          parseQuery(node);
      }
    } catch(IllegalArgumentException e) {

    }
    if(queryBuilder == null) queryBuilder = QueryBuilders.matchAllQuery();
  }

  public Map<String, Map<String, List<String>>> getTaxonomyTermsMap() {
    return taxonomyTermsMap;
  }

  private void parseQuery(ASTNode node) {
    this.node = node;
    RQLQueryBuilder builder = new RQLQueryBuilder();
    queryBuilder = node.accept(builder);
  }

  private void parseLimit(ASTNode node) {
    this.node = node;
    RQLLimitBuilder limit = new RQLLimitBuilder();
    boolean result = node.accept(limit);
    if(result) {
      from = limit.getFrom();
      size = limit.getSize();
    }
  }

  private void parseSort(ASTNode node) {
    this.node = node;
    RQLSortBuilder sort = new RQLSortBuilder();
    sortBuilder = node.accept(sort);
  }

  private void parseAggregate(ASTNode node) {
    this.node = node;
    RQLAggregateBuilder aggregate = new RQLAggregateBuilder();
    if(node.accept(aggregate)) {
      aggregations = aggregate.getAggregations();
      aggregationBuckets = aggregate.getAggregationBuckets();
    }
  }

  @Override
  public boolean hasQueryBuilder() {
    return queryBuilder != null;
  }

  @Override
  public QueryBuilder getQueryBuilder() {
    return queryBuilder;
  }

  @Override
  public void setQueryBuilder(QueryBuilder queryBuilder) {
    this.queryBuilder = queryBuilder;
  }

  @Override
  public SortBuilder getSortBuilder() {
    return sortBuilder;
  }

  @Override
  public int getFrom() {
    return from;
  }

  @Override
  public int getSize() {
    return size;
  }

  @Override
  public List<String> getAggregationBuckets() {
    if(aggregationBuckets == null) aggregationBuckets = Lists.newArrayList();
    return aggregationBuckets;
  }

  @Override
  public List<String> getAggregations() {
    return aggregations;
  }

  private class RQLQueryBuilder implements SimpleASTVisitor<QueryBuilder> {

    @Override
    public QueryBuilder visit(ASTNode node) {
      try {
        RQLNode type = RQLNode.getType(node.getName());
        switch(type) {
          case AND:
            return visitAnd(node);
          case OR:
            return visitOr(node);
          case IN:
            return visitIn(node);
          case OUT:
            return visitOut(node);
          case NOT:
            return visitNot(node);
          case EQ:
            return visitEq(node);
          case LE:
            return visitLe(node);
          case LT:
            return visitLt(node);
          case GE:
            return visitGe(node);
          case GT:
            return visitGt(node);
          case BETWEEN:
            return visitBetween(node);
          case MATCH:
            return visitMatch(node);
          case EXISTS:
            return visitExists(node);
          case MISSING:
            return visitMissing(node);
          default:
        }
      } catch(IllegalArgumentException e) {
        // ignore
      }
      return null;
    }

    private QueryBuilder visitAnd(ASTNode node) {
      QueryBuilder left = visit((ASTNode) node.getArgument(0));
      QueryBuilder right = visit((ASTNode) node.getArgument(1));
      return QueryBuilders.boolQuery().must(left).must(right);
    }

    private QueryBuilder visitOr(ASTNode node) {
      QueryBuilder left = visit((ASTNode) node.getArgument(0));
      QueryBuilder right = visit((ASTNode) node.getArgument(1));
      return QueryBuilders.boolQuery().should(left).should(right);
    }

    private QueryBuilder visitIn(ASTNode node) {
      String field = node.getArgument(0).toString();
      Object terms = node.getArgument(1);
      visitField(field, terms instanceof Collection ? ((Collection<Object>) terms).stream().map(Object::toString)
        .collect(Collectors.toList()) : Collections.singleton(terms.toString()));
      return QueryBuilders.termsQuery(field, terms instanceof Collection ? (Collection) terms : terms);
    }

    private QueryBuilder visitOut(ASTNode node) {
      String field = node.getArgument(0).toString();
      Object terms = node.getArgument(1);
      return QueryBuilders.boolQuery()
        .mustNot(QueryBuilders.termsQuery(field, terms instanceof Collection ? (Collection) terms : terms));
    }

    private QueryBuilder visitNot(ASTNode node) {
      QueryBuilder expr = visit((ASTNode) node.getArgument(0));
      return QueryBuilders.boolQuery().mustNot(expr);
    }

    private QueryBuilder visitEq(ASTNode node) {
      String field = node.getArgument(0).toString();
      Object term = node.getArgument(1);
      visitField(field, Collections.singleton(term.toString()));
      return QueryBuilders.termQuery(field, term);
    }

    private QueryBuilder visitLe(ASTNode node) {
      String field = node.getArgument(0).toString();
      Object value = node.getArgument(1);
      visitField(field);
      return QueryBuilders.rangeQuery(field).lte(value);
    }

    private QueryBuilder visitLt(ASTNode node) {
      String field = node.getArgument(0).toString();
      Object value = node.getArgument(1);
      visitField(field);
      return QueryBuilders.rangeQuery(field).lt(value);
    }

    private QueryBuilder visitGe(ASTNode node) {
      String field = node.getArgument(0).toString();
      Object value = node.getArgument(1);
      visitField(field);
      return QueryBuilders.rangeQuery(field).gte(value);
    }

    private QueryBuilder visitGt(ASTNode node) {
      String field = node.getArgument(0).toString();
      Object value = node.getArgument(1);
      visitField(field);
      return QueryBuilders.rangeQuery(field).gt(value);
    }

    private QueryBuilder visitBetween(ASTNode node) {
      String field = node.getArgument(0).toString();
      visitField(field);
      ArrayList<Object> values = (ArrayList<Object>) node.getArgument(1);
      return QueryBuilders.rangeQuery(field).gte(values.get(0)).lt(values.get(1));
    }

    private QueryBuilder visitMatch(ASTNode node) {
      if(node.getArgumentsSize() == 0) return QueryBuilders.matchAllQuery();
      // if there is only one argument, the fields to be matched are the default ones
      // otherwise, the first argument can be the field name or a list of filed names
      if(node.getArgumentsSize() == 1) return QueryBuilders.queryStringQuery(node.getArgument(0).toString());
      QueryStringQueryBuilder builder = QueryBuilders.queryStringQuery(node.getArgument(1).toString());
      if(node.getArgument(1) instanceof ArrayList) {
        ArrayList<Object> fields = (ArrayList<Object>) node.getArgument(1);
        fields.stream().map(Object::toString).forEach(builder::field);
      } else {
        builder.field(node.getArgument(0).toString());
      }
      return builder;
    }

    private QueryBuilder visitExists(ASTNode node) {
      String field = node.getArgument(0).toString();
      visitField(field);
      return QueryBuilders.existsQuery(field);
    }

    private QueryBuilder visitMissing(ASTNode node) {
      String field = node.getArgument(0).toString();
      visitField(field);
      return QueryBuilders.missingQuery(field);
    }

    private void visitField(String field) {
      visitField(field, null);
    }

    private void visitField(String field, Collection<String> terms) {
      if(!isAttributeField(field)) return;
      AttributeKey key = AttributeKey.from(field.replaceAll("^attributes\\.", "").replaceAll("\\.und$", ""));
      if(!key.hasNamespace()) return;

      if(!taxonomyTermsMap.containsKey(key.getNamespace())) {
        taxonomyTermsMap.put(key.getNamespace(), Maps.newHashMap());
      }
      Map<String, List<String>> vocMap = taxonomyTermsMap.get(key.getNamespace());
      if(!vocMap.containsKey(key.getName())) {
        vocMap.put(key.getName(), Lists.newArrayList());
      }

      if(terms != null) {
        vocMap.get(key.getName()).addAll(terms);
      }
    }

    private boolean isAttributeField(String field) {
      return field.startsWith("attributes.") && field.endsWith(".und");
    }
  }

  private static class RQLLimitBuilder implements SimpleASTVisitor<Boolean> {
    private int from = DEFAULT_FROM;

    private int size = DEFAULT_SIZE;

    public int getFrom() {
      return from;
    }

    public int getSize() {
      return size;
    }

    @Override
    public Boolean visit(ASTNode node) {
      try {
        RQLNode type = RQLNode.getType(node.getName());
        switch(type) {
          case LIMIT:
            from = (Integer) node.getArgument(0);
            if(node.getArgumentsSize() > 1) size = (Integer) node.getArgument(1);
            return Boolean.TRUE;
          default:
        }
      } catch(IllegalArgumentException e) {
        // ignore
      }
      return Boolean.FALSE;
    }
  }

  private static class RQLSortBuilder implements SimpleASTVisitor<SortBuilder> {

    @Override
    public SortBuilder visit(ASTNode node) {
      try {
        RQLNode type = RQLNode.getType(node.getName());
        switch(type) {
          case SORT:
            String arg = node.getArgument(0).toString();
            if(arg.startsWith("-")) return SortBuilders.fieldSort(arg.substring(1)).order(SortOrder.DESC);
            else if(arg.startsWith("+")) return SortBuilders.fieldSort(arg.substring(1)).order(SortOrder.ASC);
            else return SortBuilders.fieldSort(arg).order(SortOrder.ASC);
        }
      } catch(IllegalArgumentException e) {
        // ignore
      }
      return null;
    }
  }

  private static class RQLAggregateBuilder implements SimpleASTVisitor<Boolean> {

    private List<String> aggregations = Lists.newArrayList();

    private List<String> aggregationBuckets = Lists.newArrayList();

    public List<String> getAggregations() {
      return aggregations;
    }

    public List<String> getAggregationBuckets() {
      return aggregationBuckets;
    }

    @Override
    public Boolean visit(ASTNode node) {
      try {
        RQLNode type = RQLNode.getType(node.getName());
        switch(type) {
          case AGGREGATE:
            if(node.getArgumentsSize() == 0) return Boolean.TRUE;
            node.getArguments().stream().filter(a -> a instanceof String).map(Object::toString)
              .forEach(aggregations::add);
            node.getArguments().stream().filter(a -> a instanceof ASTNode).map(a -> (ASTNode) a)
              .filter(a -> RQLNode.BUCKET.name().equalsIgnoreCase(a.getName()))
              .forEach(a -> a.getArguments().stream().map(Object::toString).forEach(aggregationBuckets::add));
            return Boolean.TRUE;
          default:
        }
      } catch(IllegalArgumentException e) {
        // ignore
      }
      return Boolean.FALSE;
    }
  }
}
