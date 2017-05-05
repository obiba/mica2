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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import net.jazdw.rql.parser.ASTNode;
import net.jazdw.rql.parser.RQLParser;

import org.elasticsearch.common.Strings;
import org.obiba.mica.micaConfig.service.TaxonomyService;
import org.obiba.mica.search.mapping.IndexFieldMappingService;
import org.obiba.mica.search.queries.JoinQueryWrapper;
import org.obiba.mica.search.queries.QueryWrapper;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static java.util.Arrays.asList;

@Component
@Scope("prototype")
public class JoinRQLQueryWrapper implements JoinQueryWrapper {

  @Inject
  private IndexFieldMappingService indexFieldMappingService;

  @Inject
  private TaxonomyService taxonomyService;

  private ASTNode node;

  private boolean withFacets;

  private String locale = DEFAULT_LOCALE;

  private RQLQueryWrapper variableQueryWrapper;

  private RQLQueryWrapper datasetQueryWrapper;

  private RQLQueryWrapper studyQueryWrapper;

  private RQLQueryWrapper networkQueryWrapper;

  private List<RQLNode> nodeTypes = new ArrayList<>();

  public JoinRQLQueryWrapper() {}

  public void initialize(String rql) {
    String rqlStr = rql == null ? "" : rql;
    RQLParser parser = new RQLParser(new RQLConverter());
    node = parser.parse(rqlStr);
    initializeLocale(node);

    if(Strings.isNullOrEmpty(node.getName()))
      node.getArguments().stream().filter(a -> a instanceof ASTNode).map(a -> (ASTNode) a).forEach(this::initialize);
    else initialize(node);

    // make sure we have initialize everyone
    if (variableQueryWrapper == null) {
      variableQueryWrapper = new RQLQueryWrapper("");
    }
    if (datasetQueryWrapper == null) {
      datasetQueryWrapper = new RQLQueryWrapper("");
    }
    if (studyQueryWrapper == null) {
      studyQueryWrapper = new RQLQueryWrapper("");
    }
    if (networkQueryWrapper == null) {
      networkQueryWrapper = new RQLQueryWrapper("");
    }
  }

  private void initialize(ASTNode node) {
    RQLNode rqlNode = RQLNode.valueOf(node.getName().toUpperCase());
    nodeTypes.add(rqlNode);
    switch(rqlNode) {
      case VARIABLE:
        variableQueryWrapper = new RQLQueryWrapper(node, new RqlFieldResolver(rqlNode, getVariableTaxonomies(), locale,
            indexFieldMappingService.getVariableIndexMapping()));
        break;
      case DATASET:
        datasetQueryWrapper = new RQLQueryWrapper(node, new RqlFieldResolver(rqlNode, getDatasetTaxonomies(), locale,
            indexFieldMappingService.getDatasetIndexMapping()));
        break;
      case STUDY:
        studyQueryWrapper = new RQLQueryWrapper(node, new RqlFieldResolver(rqlNode, getStudyTaxonomies(), locale,
            indexFieldMappingService.getStudyIndexMapping()));
        break;
      case NETWORK:
        networkQueryWrapper = new RQLQueryWrapper(node, new RqlFieldResolver(rqlNode, getNetworkTaxonomies(), locale,
            indexFieldMappingService.getNetworkIndexMapping()));
        break;
      case LOCALE:
        if(node.getArgumentsSize() > 0) locale = node.getArgument(0).toString();
        break;
      case FACET:
        withFacets = true;
        break;
    }
  }

  private void initializeLocale(ASTNode node) {
    if(Strings.isNullOrEmpty(node.getName())) {
      Optional<Object> localeNode = node.getArguments().stream()
        .filter(a -> a instanceof ASTNode && RQLNode.LOCALE == RQLNode.getType(((ASTNode) a).getName())).findFirst();

      if (localeNode.isPresent()) {
        initialize((ASTNode)localeNode.get());
      }
    } else if (RQLNode.LOCALE == RQLNode.getType(node.getName())){
      initialize(node);
    }
  }

  @Override
  public boolean isWithFacets() {
    return withFacets;
  }

  @Override
  public String getLocale() {
    return locale;
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

  @Override
  public boolean searchOnNetworksOnly() {
    return nodeTypes.contains(RQLNode.NETWORK) &&
      nodeTypes.stream().noneMatch(rqlNode -> asList(RQLNode.STUDY, RQLNode.DATASET, RQLNode.VARIABLE).contains(rqlNode));
  }

  //
  // Private methods
  //

  private List<Taxonomy> getVariableTaxonomies() {
    return taxonomyService.getVariableTaxonomies();
  }

  private List<Taxonomy> getDatasetTaxonomies() {
    return Collections.singletonList(taxonomyService.getDatasetTaxonomy());
  }

  private List<Taxonomy> getStudyTaxonomies() {
    return Collections.singletonList(taxonomyService.getStudyTaxonomy());
  }

  private List<Taxonomy> getNetworkTaxonomies() {
    return Collections.singletonList(taxonomyService.getNetworkTaxonomy());
  }

}
