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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.obiba.mica.search.rest.QueryDtoBuilders;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import jdk.nashorn.internal.ir.annotations.Ignore;

@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { QueryDtoBuildersTest.Config.class})
public class QueryDtoBuildersTest {

  @Test
  public void test_terms_query() throws IOException {
  }

  @Configuration
  static class Config {
  }

}
