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

import java.io.IOException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
public class RQLQueryWrapperTest {

  @Test
  public void test_rql_query_terms() throws IOException {
    String rql
      = "variable(or(in(attributes.Mlstr_area__Lifestyle_behaviours.und,(Phys_act,Tobacco)),in(attributes.Mlstr_area__Diseases.und,Neoplasms)))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    String expected = "{\n" +
      "  \"bool\" : {\n" +
      "    \"should\" : [ {\n" +
      "      \"terms\" : {\n" +
      "        \"attributes.Mlstr_area__Lifestyle_behaviours.und\" : [ [ \"Phys_act\", \"Tobacco\" ] ]\n" +
      "      }\n" +
      "    }, {\n" +
      "      \"terms\" : {\n" +
      "        \"attributes.Mlstr_area__Diseases.und\" : [ \"Neoplasms\" ]\n" +
      "      }\n" +
      "    } ]\n" +
      "  }\n" +
      "}";
    assertThat(rqlQueryWrapper.getQueryBuilder().toString()).isEqualTo(expected);
  }

  @Test
  public void test_rql_query_range() throws IOException {
    String rql = "study(and(ge(populations.selectionCriteria.ageMin,50),le(populations.selectionCriteria.ageMin,60)))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    String expected = "{\n" +
      "  \"bool\" : {\n" +
      "    \"must\" : [ {\n" +
      "      \"range\" : {\n" +
      "        \"populations.selectionCriteria.ageMin\" : {\n" +
      "          \"from\" : 50,\n" +
      "          \"to\" : null,\n" +
      "          \"include_lower\" : true,\n" +
      "          \"include_upper\" : true\n" +
      "        }\n" +
      "      }\n" +
      "    }, {\n" +
      "      \"range\" : {\n" +
      "        \"populations.selectionCriteria.ageMin\" : {\n" +
      "          \"from\" : null,\n" +
      "          \"to\" : 60,\n" +
      "          \"include_lower\" : true,\n" +
      "          \"include_upper\" : true\n" +
      "        }\n" +
      "      }\n" +
      "    } ]\n" +
      "  }\n" +
      "}";
    assertThat(rqlQueryWrapper.getQueryBuilder().toString()).isEqualTo(expected);
  }

  @Test
  public void test_rql_query_term_and_limit_and_sort() throws IOException {
    String rql = "network(eq(id,ialsa),limit(3,4),sort(-name))";
    RQLQueryWrapper rqlQueryWrapper = new RQLQueryWrapper(rql);
    assertThat(rqlQueryWrapper.hasQueryBuilder()).isTrue();
    String expectedQuery = "{\n" +
      "  \"term\" : {\n" +
      "    \"id\" : \"ialsa\"\n" +
      "  }\n" +
      "}";
    assertThat(rqlQueryWrapper.getQueryBuilder().toString()).isEqualTo(expectedQuery);
    assertThat(rqlQueryWrapper.getFrom()).isEqualTo(3);
    assertThat(rqlQueryWrapper.getSize()).isEqualTo(4);
    String expectedSort = "\n" +
      "\"name\"{\n" +
      "  \"order\" : \"desc\"\n" +
      "}";
    assertThat(rqlQueryWrapper.getSortBuilder().toString()).isEqualTo(expectedSort);
  }
}
