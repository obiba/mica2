/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.junit.Test;
import org.junit.runner.RunWith;
import org.obiba.mica.study.search.rest.QueryDtoParser;
import org.obiba.mica.web.model.MicaSearch;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { DtoParserTest.Config.class })
public class DtoParserTest {

  @Test
  public void test_query_dto_parser() {
    MicaSearch.TermsQueryDto termsDto = MicaSearch.TermsQueryDto.newBuilder().setField("access")
        .addValues("bio-samples").build();
    MicaSearch.BoolFilterQueryDto boolDto = MicaSearch.BoolFilterQueryDto.newBuilder().addTerms(termsDto).build();
    MicaSearch.FilteredQueryDto filteredDto = MicaSearch.FilteredQueryDto.newBuilder().setFilter(boolDto).build();
    MicaSearch.QueryDto quertDto = MicaSearch.QueryDto.newBuilder().setFilteredQuery(filteredDto).setFrom(0).setSize(10)
        .setDetailed(false).build();

    QueryDtoParser parser = QueryDtoParser.newParser();
//    assertThat();
    System.out.println(parser.parse(quertDto));
  }

  @Configuration
  static class Config {}

}
