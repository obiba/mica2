package org.obiba.mica.core.upgrade;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.reset;
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
    List<String> names = Arrays.asList("network", "study", "dataset", "variable", "taxonomy");
    for (String name : names) {
      reset(taxonomyConfigRepository);
      when(taxonomyConfigRepository.findOne(name)).thenReturn(getWrapper(name));
      mica220Upgrade.updateTaxonomyWithRangeCriteria(name);
      verify(taxonomyConfigRepository).save((TaxonomyEntityWrapper)argThat(new TaxonomyArgumentMatcher()));
    }

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
      List<Vocabulary> vocabularies = wrapper.getTaxonomy().getVocabularies().stream()
        .filter(v -> v.getName().endsWith("-range")).collect(Collectors.toList());

      return vocabularies.isEmpty() || vocabularies.stream()
        .filter(v -> v.getAttributeValue("alias").endsWith("-range") &&
          v.getAttributeValue("range").equals("true"))
        .count() > 0;
    }
  }
}
