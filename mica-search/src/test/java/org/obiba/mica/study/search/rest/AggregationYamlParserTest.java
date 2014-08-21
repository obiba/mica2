/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.study.search.rest;

import java.io.IOException;
import java.net.URISyntaxException;

import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.obiba.mica.search.rest.AggregationYamlParser;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

// TODO complete the tests using service mocks
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { DtoParserTest.Config.class })
public class AggregationYamlParserTest {

  @Test
  public void test_simple_yaml_parse() throws URISyntaxException, IOException {
    String filename = getClass().getResource("/aggregations.yml").toURI().toString();
    AggregationYamlParser parser = new AggregationYamlParser();
    Iterable<AbstractAggregationBuilder> result = parser.getAggregations(new ClassPathResource("aggregations.yml"));
    System.out.println(result);
  }

  @Configuration
  static class Config {}

}
