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

import java.util.Collection;
import java.util.List;

import net.jazdw.rql.parser.ASTNode;
import net.jazdw.rql.parser.RQLParser;
import net.jazdw.rql.parser.SimpleASTVisitor;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilder;
import org.obiba.mica.search.queries.QueryWrapper;

/**
 *
 */
public class RQLQueryWrapper implements QueryWrapper {

  private int from = DEFAULT_FROM;

  private int size = DEFAULT_SIZE;

  private ASTNode node;

  private QueryBuilder queryBuilder;

  public RQLQueryWrapper(String rql) {
    this(new RQLParser().parse(rql));
  }

  public RQLQueryWrapper(ASTNode node) {
    try {
      RQLNode type = RQLNode.valueOf(node.getName().toUpperCase());
      switch(type) {
        case VARIABLE:
        case DATASET:
        case STUDY:
        case NETWORK:
          parseQuery((ASTNode) node.getArgument(0));
          if(node.getArgumentsSize() > 1) parseLimit((ASTNode) node.getArgument(1));
          break;
        default:
          parseQuery(node);
      }
    } catch(IllegalArgumentException e) {

    }
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
    return null;
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
  public List<String> getAggregationGroupBy() {
    return null;
  }

  private static class RQLQueryBuilder implements SimpleASTVisitor<QueryBuilder> {

    @Override
    public QueryBuilder visit(ASTNode node) {
      try {
        RQLNode type = RQLNode.valueOf(node.getName().toUpperCase());
        switch(type) {
          case AND:
            return visitAnd(node);
          case OR:
            return visitOr(node);
          case IN:
            return visitIn(node);
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
      return QueryBuilders.termsQuery(field, terms instanceof Collection ? (Collection) terms : terms);
    }

    private QueryBuilder visitEq(ASTNode node) {
      String field = node.getArgument(0).toString();
      Object term = node.getArgument(1);
      return QueryBuilders.termQuery(field, term);
    }

    private QueryBuilder visitLe(ASTNode node) {
      String field = node.getArgument(0).toString();
      Object value = node.getArgument(1);
      return QueryBuilders.rangeQuery(field).lte(value);
    }

    private QueryBuilder visitLt(ASTNode node) {
      String field = node.getArgument(0).toString();
      Object value = node.getArgument(1);
      return QueryBuilders.rangeQuery(field).lt(value);
    }

    private QueryBuilder visitGe(ASTNode node) {
      String field = node.getArgument(0).toString();
      Object value = node.getArgument(1);
      return QueryBuilders.rangeQuery(field).gte(value);
    }

    private QueryBuilder visitGt(ASTNode node) {
      String field = node.getArgument(0).toString();
      Object value = node.getArgument(1);
      return QueryBuilders.rangeQuery(field).gt(value);
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
        RQLNode type = RQLNode.valueOf(node.getName().toUpperCase());
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
}
