/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
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
import java.util.Optional;
import java.util.stream.Collectors;

import net.jazdw.rql.parser.ASTNode;
import net.jazdw.rql.parser.RQLParser;
import net.jazdw.rql.parser.SimpleASTVisitor;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.obiba.mica.core.domain.AttributeKey;
import org.obiba.mica.search.queries.QueryWrapper;
import org.obiba.mica.search.queries.rql.RqlFieldResolver.FieldData;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.TaxonomyEntity;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 *
 */
public class RQLQueryWrapper implements QueryWrapper {

  private final RqlFieldResolver rqlFieldResolver;

  private int from = DEFAULT_FROM;

  private int size = DEFAULT_SIZE;

  private ASTNode node;

  private QueryBuilder queryBuilder;

  private List<SortBuilder> sortBuilders;

  private List<String> aggregations;

  private List<String> aggregationBuckets;

  private final Map<String, Map<String, List<String>>> taxonomyTermsMap = Maps.newHashMap();

  @VisibleForTesting
  RQLQueryWrapper(String rql) {
    this(new RQLParser(new RQLConverter()).parse(rql), new RqlFieldResolver(null, Collections.emptyList(), "en",
        null ));
  }

  public RQLQueryWrapper(ASTNode node, RqlFieldResolver rqlFieldResolver) {
    this.rqlFieldResolver = rqlFieldResolver;
    parseNode(node);
  }

  public Map<String, Map<String, List<String>>> getTaxonomyTermsMap() {
    return taxonomyTermsMap;
  }

  private void parseNode(ASTNode node) {
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
  }

  private void parseQuery(ASTNode node) {
    this.node = node;
    RQLQueryBuilder builder = new RQLQueryBuilder(rqlFieldResolver);
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
    RQLSortBuilder sort = new RQLSortBuilder(rqlFieldResolver);
    sortBuilders = node.accept(sort);
  }

  private void parseAggregate(ASTNode node) {
    this.node = node;
    RQLAggregateBuilder aggregate = new RQLAggregateBuilder(rqlFieldResolver);
    if(node.accept(aggregate)) {
      aggregations = aggregate.getAggregations();
      aggregationBuckets = aggregate.getAggregationBuckets();
    }
  }

  @VisibleForTesting
  ASTNode getNode() {
    return node;
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
  public List<SortBuilder> getSortBuilders() {
    return sortBuilders;
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

  private abstract class RQLBuilder<T> implements SimpleASTVisitor<T> {

    protected final RqlFieldResolver rqlFieldResolver;

    RQLBuilder(RqlFieldResolver rqlFieldResolver) {
      this.rqlFieldResolver = rqlFieldResolver;
    }

    protected FieldData resolveField(String rqlField) {
      return rqlFieldResolver.resolveField(rqlField);
    }

    protected FieldData resolveFieldUnanalyzed(String rqlField) {
      return rqlFieldResolver.resolveFieldUnanalyzed(rqlField);
    }

    protected Vocabulary getVocabulary(String taxonomyName, String vocabularyName) {
      Optional<Taxonomy> taxonomy = rqlFieldResolver.getTaxonomies().stream()
        .filter(t -> t.getName().equals(taxonomyName)).findFirst();
      if(taxonomy.isPresent() && taxonomy.get().hasVocabularies()) {
        Optional<Vocabulary> vocabulary = taxonomy.get().getVocabularies().stream()
          .filter(v -> v.getName().equals(vocabularyName)).findFirst();
        if(vocabulary.isPresent()) {
          return vocabulary.get();
        }
      }
      return null;
    }

  }

  private class RQLQueryBuilder extends RQLBuilder<QueryBuilder> {
    RQLQueryBuilder(RqlFieldResolver rqlFieldResolver) {
      super(rqlFieldResolver);
    }

    @Override
    public QueryBuilder visit(ASTNode node) {
      try {
        RQLNode type = RQLNode.getType(node.getName());
        switch(type) {
          case AND:
            return visitAnd(node);
          case NAND:
            return visitNand(node);
          case OR:
            return visitOr(node);
          case NOR:
            return visitNor(node);
          case CONTAINS:
            return visitContains(node);
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

    private QueryBuilder visitNand(ASTNode node) {
      QueryBuilder left = visit((ASTNode) node.getArgument(0));
      QueryBuilder right = visit((ASTNode) node.getArgument(1));
      return QueryBuilders.boolQuery().mustNot(QueryBuilders.boolQuery().must(left).must(right));
    }

    private QueryBuilder visitOr(ASTNode node) {
      QueryBuilder left = visit((ASTNode) node.getArgument(0));
      QueryBuilder right = visit((ASTNode) node.getArgument(1));
      return QueryBuilders.boolQuery().should(left).should(right);
    }

    private QueryBuilder visitNor(ASTNode node) {
      QueryBuilder left = visit((ASTNode) node.getArgument(0));
      QueryBuilder right = visit((ASTNode) node.getArgument(1));
      return QueryBuilders.boolQuery().mustNot(QueryBuilders.boolQuery().should(left).should(right));
    }

    private QueryBuilder visitContains(ASTNode node) {
      FieldData data = resolveField(node.getArgument(0).toString());
      String field = data.getField();
      Object args = node.getArgument(1);
      Collection<String> terms;
      terms = args instanceof Collection ? ((Collection<Object>) args).stream().map(Object::toString)
        .collect(Collectors.toList()) : Collections.singleton(args.toString());
      visitField(field, terms);
      BoolQueryBuilder builder = QueryBuilders.boolQuery();
      terms.forEach(t -> builder.must(QueryBuilders.termQuery(field, t)));
      return builder;
    }

    private QueryBuilder visitIn(ASTNode node) {
      FieldData data = resolveField(node.getArgument(0).toString());
      String field = data.getField();
      if(data.isRange()) {
        return visitInRangeInternal(data, node.getArgument(1));
      }

      Object terms = node.getArgument(1);
      visitField(field, terms instanceof Collection ? ((Collection<Object>) terms).stream().map(Object::toString)
        .collect(Collectors.toList()) : Collections.singleton(terms.toString()));
      return QueryBuilders.termsQuery(field, terms instanceof Collection ? (Collection) terms : terms);
    }

    private QueryBuilder visitInRangeInternal(FieldData data, Object rangesArgument) {
      Collection<String> ranges = rangesArgument instanceof Collection ? ((Collection<Object>) rangesArgument).stream()
        .map(Object::toString).collect(Collectors.toList()) : Collections.singleton(rangesArgument.toString());

      BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
      ranges.forEach(range -> {
        RangeQueryBuilder builder = QueryBuilders.rangeQuery(data.getField());
        String[] values = range.split(":");
        if(values.length < 2) {
          throw new IllegalArgumentException("Invalid range format: " + range);
        }

        if (!"*".equals(values[0]) || !"*".equals(values[1])) {
          if("*".equals(values[0])) {
            builder.lt(Double.valueOf(values[1]));
          } else if("*".equals(values[1])) {
            builder.gte(Double.valueOf(values[0]));
          } else {
            builder.gte(Double.valueOf(values[0]));
            builder.lt(Double.valueOf(values[1]));
          }
        }

        boolQueryBuilder.should(builder);
      });

      return boolQueryBuilder;
    }

    private QueryBuilder visitOut(ASTNode node) {
      String field = resolveField(node.getArgument(0).toString()).getField();
      Object terms = node.getArgument(1);
      return QueryBuilders.boolQuery()
        .mustNot(QueryBuilders.termsQuery(field, terms instanceof Collection ? (Collection) terms : terms));
    }

    private QueryBuilder visitNot(ASTNode node) {
      QueryBuilder expr = visit((ASTNode) node.getArgument(0));
      return QueryBuilders.boolQuery().mustNot(expr);
    }

    private QueryBuilder visitEq(ASTNode node) {
      String field = resolveField(node.getArgument(0).toString()).getField();
      Object term = node.getArgument(1);
      visitField(field, Collections.singleton(term.toString()));
      return QueryBuilders.termQuery(field, term);
    }

    private QueryBuilder visitLe(ASTNode node) {
      String field = resolveField(node.getArgument(0).toString()).getField();
      Object value = node.getArgument(1);
      visitField(field);
      return QueryBuilders.rangeQuery(field).lte(value);
    }

    private QueryBuilder visitLt(ASTNode node) {
      String field = resolveField(node.getArgument(0).toString()).getField();
      Object value = node.getArgument(1);
      visitField(field);
      return QueryBuilders.rangeQuery(field).lt(value);
    }

    private QueryBuilder visitGe(ASTNode node) {
      String field = resolveField(node.getArgument(0).toString()).getField();
      Object value = node.getArgument(1);
      visitField(field);
      return QueryBuilders.rangeQuery(field).gte(value);
    }

    private QueryBuilder visitGt(ASTNode node) {
      String field = resolveField(node.getArgument(0).toString()).getField();
      Object value = node.getArgument(1);
      visitField(field);
      return QueryBuilders.rangeQuery(field).gt(value);
    }

    private QueryBuilder visitBetween(ASTNode node) {
      String field = resolveField(node.getArgument(0).toString()).getField();
      visitField(field);
      ArrayList<Object> values = (ArrayList<Object>) node.getArgument(1);
      return QueryBuilders.rangeQuery(field).gte(values.get(0)).lt(values.get(1));
    }

    private QueryBuilder visitMatch(ASTNode node) {
      if(node.getArgumentsSize() == 0) return QueryBuilders.matchAllQuery();
      // if there is only one argument, the fields to be matched are the default ones
      // otherwise, the first argument can be the field name or a list of filed names
      if(node.getArgumentsSize() == 1) return QueryBuilders.queryStringQuery(node.getArgument(0).toString());
      QueryStringQueryBuilder builder = QueryBuilders.queryStringQuery(node.getArgument(0).toString());
      if(node.getArgument(1) instanceof ArrayList) {
        ArrayList<Object> fields = (ArrayList<Object>) node.getArgument(1);
        fields.stream().map(Object::toString).forEach(f -> builder.field(resolveField(f).getField()));
      } else {
        builder.field(resolveField(node.getArgument(1).toString()).getField());
      }
      return builder;
    }

    private QueryBuilder visitExists(ASTNode node) {
      String field = resolveField(node.getArgument(0).toString()).getField();
      visitField(field);
      return QueryBuilders.existsQuery(field);
    }

    private QueryBuilder visitMissing(ASTNode node) {
      String field = resolveField(node.getArgument(0).toString()).getField();
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
      } else {
        // add all terms from taxonomy vocabulary
        Vocabulary vocabulary = getVocabulary(key.getNamespace(), key.getName());
        if(vocabulary != null && vocabulary.hasTerms()) {
          vocMap.get(key.getName())
            .addAll(vocabulary.getTerms().stream().map(TaxonomyEntity::getName).collect(Collectors.toList()));
        }
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

  private class RQLSortBuilder extends RQLBuilder<List<SortBuilder>> {
    RQLSortBuilder(RqlFieldResolver rqlFieldResolver) {
      super(rqlFieldResolver);
    }

    @Override
    public List<SortBuilder> visit(ASTNode node) {
      try {
        RQLNode type = RQLNode.getType(node.getName());
        switch(type) {
          case SORT:
            List<SortBuilder> sortBuilders = Lists.newArrayList();
            if (node.getArgumentsSize() >= 1) {
              for(int i = 0; i < node.getArgumentsSize(); i++) {
                SortBuilder sortBuilder = processArgument(node.getArgument(i).toString());
                ((FieldSortBuilder) sortBuilder).unmappedType("string");
                sortBuilder.missing("_last");
                sortBuilders.add(sortBuilder);
              }
            }
            return sortBuilders;
        }
      } catch(IllegalArgumentException e) {
        // ignore
      }
      return null;
    }

    private SortBuilder processArgument(String arg) {
      if(arg.startsWith("-"))
        return SortBuilders.fieldSort(resolveFieldUnanalyzed(arg.substring(1)).getField()).order(SortOrder.DESC);
      else if(arg.startsWith("+"))
        return SortBuilders.fieldSort(resolveFieldUnanalyzed(arg.substring(1)).getField()).order(SortOrder.ASC);
      else return SortBuilders.fieldSort(resolveFieldUnanalyzed(arg).getField()).order(SortOrder.ASC);
    }

  }

  private class RQLAggregateBuilder extends RQLBuilder<Boolean> {
    RQLAggregateBuilder(RqlFieldResolver rqlFieldResolver) {
      super(rqlFieldResolver);
    }

    private List<String> aggregations = Lists.newArrayList();

    private List<String> aggregationBuckets = Lists.newArrayList();

    public List<String> getAggregations() {
      return aggregations.stream().map(a -> resolveField(a).getField()).collect(Collectors.toList());
    }

    public List<String> getAggregationBuckets() {
      return aggregationBuckets.stream().map(a -> resolveField(a).getField()).collect(Collectors.toList());
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
              .forEach(a -> {
                switch (RQLNode.getType(a.getName())) {
                  case BUCKET:
                    a.getArguments().stream().map(Object::toString).forEach(aggregationBuckets::add);
                    break;
                  case RE:
                    a.getArguments().stream().map(Object::toString).forEach(aggregations::add);
                    break;
                }
              });
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
