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

import java.util.Arrays;
import java.util.stream.Collectors;

import net.jazdw.rql.parser.ASTNode;

import org.elasticsearch.common.Strings;
import org.obiba.mica.spi.search.rql.RQLNode;

public class RQLQueryBuilder {

  private ASTNode root;

  private RQLQueryBuilder() {
    root = new ASTNode("");
  }

  public static RQLQueryBuilder newInstance() {
    return new RQLQueryBuilder();
  }

  public RQLQueryBuilder locale(String locale) {
    root.createChildNode(RQLNode.LOCALE.name().toLowerCase(), Arrays.asList(locale));
    return this;
  }

  public RQLQueryBuilder target(ASTNode node) {
    root.addArgument(node);
    return this;
  }

  public ASTNode build() {
    return root;
  }

  public String buildArgsAsString() {
    return root.getArguments().stream().map(n -> n.toString()).collect(Collectors.joining(","));
  }

  public static class TargetQueryBuilder {

    private ASTNode target;

    TargetQueryBuilder(RQLNode targetNode) {
      target = new ASTNode(targetNode.name().toLowerCase());
    }

    public static TargetQueryBuilder studyInstance() {
      return newInstance(RQLNode.STUDY);
    }

    public static TargetQueryBuilder networkInstance() {
      return newInstance(RQLNode.NETWORK);
    }

    public static TargetQueryBuilder datasetInstance() {
      return newInstance(RQLNode.DATASET);
    }

    public static TargetQueryBuilder variableInstance() {
      return newInstance(RQLNode.VARIABLE);
    }

    private static TargetQueryBuilder newInstance(RQLNode targetNode) {
      return new TargetQueryBuilder(targetNode);
    }

    public TargetQueryBuilder limit(int from, int size) {
      target.createChildNode(RQLNode.LIMIT.name().toLowerCase(), Arrays.asList(from, size));
      return this;
    }

    public TargetQueryBuilder exists(String field) {
      target.createChildNode(RQLNode.EXISTS.name().toLowerCase(), Arrays.asList(field));
      return this;
    }

    public TargetQueryBuilder sort(String name, String order) {
      String sortOrder = Strings.isNullOrEmpty(order) || "asc".equals(order) ? "+" : "-";

      target.createChildNode(RQLNode.SORT.name().toLowerCase(),
        Arrays.asList(String.format("%s%s", sortOrder, name)));

      return this;
    }

    public ASTNode build() {
      return target;
    }

  }
}
