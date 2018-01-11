/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.upgrade;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.obiba.mica.core.domain.TaxonomyEntityWrapper;
import org.obiba.mica.micaConfig.repository.TaxonomyConfigRepository;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Mica220UpgradeTest {

  @InjectMocks
  private Mica220Upgrade mica220Upgrade;

  @Mock
  private TaxonomyConfigRepository taxonomyConfigRepository;

  @Test
  public void testUpdateRangeTaxonomies() throws IOException {
    when(taxonomyConfigRepository.findOne("study")).thenReturn(getWrapper("study"));
    mica220Upgrade.updateTaxonomyWithRangeCriteria("study");
    verify(taxonomyConfigRepository).save((TaxonomyEntityWrapper)argThat(new TaxonomyArgumentMatcher()));
  }

  private TaxonomyEntityWrapper getWrapper(String name) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    Taxonomy taxonomy = mapper.readValue(new File("src/test/resources/config/mica-"+name+".json"), Taxonomy.class);
    TaxonomyEntityWrapper wrapper = new TaxonomyEntityWrapper();
    wrapper.setTarget(name);
    wrapper.setTaxonomy(taxonomy);
    return wrapper;
  }

  class TaxonomyArgumentMatcher extends ArgumentMatcher {

    @Override
    public boolean matches(Object object) {
      TaxonomyEntityWrapper wrapper = (TaxonomyEntityWrapper)object;
      Vocabulary vocabulary = wrapper.getTaxonomy().getVocabulary("numberOfParticipants-participant-range");
      return vocabulary.getAttributeValue("alias").endsWith("-range") &&
          vocabulary.getAttributeValue("range").equals("true");
    }
  }
}
