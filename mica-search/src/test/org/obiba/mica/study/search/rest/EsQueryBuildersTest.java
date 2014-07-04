/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.obiba.mica.study.search.rest.EsQueryBuilders;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { EsQueryBuildersTest.Config.class})
public class EsQueryBuildersTest {

  @Test
  public void test_filtered_term_query() throws IOException {
    EsQueryBuilders.EsBoolTermsQueryBuilder builder = EsQueryBuilders.EsBoolTermsQueryBuilder.newBuilder();
    builder.addTerm("toto", "tata");
    String expected = "{\"query\":{\"bool\":{\"must\":[{\"term\":{\"toto\":\"tata\"}}]}}}";
    System.out.println(builder.build());
  }

  @Test
  public void test_filtered_terms_query() throws IOException {
    EsQueryBuilders.EsBoolTermsQueryBuilder builder = EsQueryBuilders.EsBoolTermsQueryBuilder.newBuilder();
    builder.addTerms("toto", "Zorro", "Zarra");
    String expected = "{\"query\":{\"bool\":{\"must\":[{\"term\":{\"toto\":\"tata\"}}]}}}";
    System.out.println(builder.build());
  }

  @Configuration
  static class Config {
  }

}
