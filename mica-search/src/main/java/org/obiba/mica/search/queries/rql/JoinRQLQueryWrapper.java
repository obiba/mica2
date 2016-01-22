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

import java.util.List;
import java.util.stream.Collectors;

import net.jazdw.rql.parser.ASTNode;
import net.jazdw.rql.parser.RQLParser;

import org.elasticsearch.common.Strings;
import org.obiba.mica.search.queries.JoinQueryWrapper;
import org.obiba.mica.search.queries.QueryWrapper;

import com.google.common.collect.Lists;

/**
 *
 */
public class JoinRQLQueryWrapper implements JoinQueryWrapper {

  private ASTNode node;

  private List<String> facets;

  private RQLQueryWrapper variableQueryWrapper;

  private RQLQueryWrapper datasetQueryWrapper;

  private RQLQueryWrapper studyQueryWrapper;

  private RQLQueryWrapper networkQueryWrapper;

  public JoinRQLQueryWrapper(String rql) {
    RQLParser parser = new RQLParser();
    node = parser.parse(rql);
    if(Strings.isNullOrEmpty(node.getName()))
      node.getArguments().stream().filter(a -> a instanceof ASTNode).map(a -> (ASTNode) a).forEach(this::initialize);
    else initialize(node);
  }

  private void initialize(ASTNode node) {

    switch(RQLNode.valueOf(node.getName().toUpperCase())) {
      case VARIABLE:
        variableQueryWrapper = new RQLQueryWrapper(node);
        break;
      case DATASET:
        datasetQueryWrapper = new RQLQueryWrapper(node);
        break;
      case STUDY:
        studyQueryWrapper = new RQLQueryWrapper(node);
        break;
      case NETWORK:
        networkQueryWrapper = new RQLQueryWrapper(node);
        break;
      case FACET:
        facets = Lists.newArrayList();
        if(node.getArgumentsSize() == 1) facets.add(node.getArgument(0).toString());
        if(node.getArgumentsSize() > 1)
          facets.addAll(node.getArguments().stream().map(Object::toString).collect(Collectors.toList()));
    }
  }

  @Override
  public boolean isWithFacets() {
    return facets != null;
  }

  @Override
  public String getLocale() {
    return DEFAULT_LOCALE;
  }

  @Override
  public QueryWrapper getVariableQueryWrapper() {
    return variableQueryWrapper;
  }

  @Override
  public QueryWrapper getDatasetQueryWrapper() {
    return datasetQueryWrapper;
  }

  @Override
  public QueryWrapper getStudyQueryWrapper() {
    return studyQueryWrapper;
  }

  @Override
  public QueryWrapper getNetworkQueryWrapper() {
    return networkQueryWrapper;
  }
}
