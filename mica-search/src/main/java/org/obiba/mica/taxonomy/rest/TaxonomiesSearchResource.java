/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
 *
 *  This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.taxonomy.rest;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.obiba.mica.core.service.PublishedDocumentService;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.taxonomy.EsTaxonomyTermService;
import org.obiba.mica.taxonomy.TaxonomyResolver;
import org.obiba.opal.web.model.Opal;
import org.obiba.opal.web.taxonomy.Dtos;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

@Component
@Scope("request")
@Path("/taxonomies")
@RequiresAuthentication
public class TaxonomiesSearchResource {

  protected static final String DEFAULT_SORT = "id";

  @Inject
  private EsTaxonomyTermService esTaxonomyTermService;

  @Inject
  private OpalService opalService;

  @GET
  @Path("/_filter")
  public List<Opal.TaxonomyDto> search(@QueryParam("query") String query) {
    if(Strings.isNullOrEmpty(query)) return opalService.getTaxonomyDtos();

    PublishedDocumentService.Documents<String> termIds = esTaxonomyTermService
      .find(0, Integer.MAX_VALUE, DEFAULT_SORT, "asc", null, query);

    List<Opal.TaxonomyDto> results = Lists.newArrayList();

    Map<String, List<String>> vocabularyNames = TaxonomyResolver.asMap(termIds.getList());

    opalService.getTaxonomies().stream().filter(t -> vocabularyNames.containsKey(t.getName())).forEach(taxo -> {
      Opal.TaxonomyDto.Builder tBuilder = Dtos.asDto(taxo, false).toBuilder();
      taxo.getVocabularies().stream().filter(v -> vocabularyNames.get(taxo.getName()).contains(v.getName()))
        .forEach(voc -> {
          Opal.VocabularyDto.Builder vBuilder = Dtos.asDto(voc, false).toBuilder();
          voc.getTerms().stream()
            .filter(t -> termIds.getList().contains(TaxonomyResolver.asId(taxo.getName(), voc.getName(), t.getName())))
            .forEach(term -> {
              vBuilder.addTerms(Dtos.asDto(term));
            });
          tBuilder.addVocabularies(vBuilder);
        });
      results.add(tBuilder.build());
    });

    return results;
  }

}
