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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import net.jazdw.rql.parser.ASTNode;
import net.jazdw.rql.parser.RQLParser;

import org.elasticsearch.common.Strings;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.search.queries.JoinQueryWrapper;
import org.obiba.mica.search.queries.QueryWrapper;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class JoinRQLQueryWrapper implements JoinQueryWrapper {

  @Inject
  private MicaConfigService micaConfigService;

  @Inject
  private OpalService opalService;

  private ASTNode node;

  private boolean withFacets;

  private String locale = DEFAULT_LOCALE;

  private RQLQueryWrapper variableQueryWrapper;

  private RQLQueryWrapper datasetQueryWrapper;

  private RQLQueryWrapper studyQueryWrapper;

  private RQLQueryWrapper networkQueryWrapper;

  public JoinRQLQueryWrapper() {}

  public void initialize(String rql) {
    String rqlStr = rql == null ? "" : rql;
    RQLParser parser = new RQLParser();
    node = parser.parse(rqlStr);
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

    switch(RQLNode.valueOf(node.getName().toUpperCase())) {
      case VARIABLE:
        variableQueryWrapper = new RQLQueryWrapper(node, getVariableTaxonomies());
        break;
      case DATASET:
        datasetQueryWrapper = new RQLQueryWrapper(node, getDatasetTaxonomies());
        break;
      case STUDY:
        studyQueryWrapper = new RQLQueryWrapper(node, getStudyTaxonomies());
        break;
      case NETWORK:
        networkQueryWrapper = new RQLQueryWrapper(node, getNetworkTaxonomies());
        break;
      case LOCALE:
        if(node.getArgumentsSize() > 0) locale = node.getArgument(0).toString();
        break;
      case FACET:
        withFacets = true;
        break;
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

  //
  // Private methods
  //

  private List<Taxonomy> getVariableTaxonomies() {
    return Stream.concat(opalService.getTaxonomies().stream(), Stream.of(micaConfigService.getVariableTaxonomy()))
      .collect(Collectors.toList());
  }

  private List<Taxonomy> getDatasetTaxonomies() {
    return Collections.singletonList(micaConfigService.getDatasetTaxonomy());
  }

  private List<Taxonomy> getStudyTaxonomies() {
    return Collections.singletonList(micaConfigService.getStudyTaxonomy());
  }

  private List<Taxonomy> getNetworkTaxonomies() {
    return Collections.singletonList(micaConfigService.getNetworkTaxonomy());
  }

}
