/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.queries.rql;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RqlQueryBuilderTest {

  @Test
  public void test_target_builder() {
    assertThat(RQLQueryBuilder.TargetQueryBuilder.networkInstance().build().toString()).isEqualTo("network()");
    assertThat(RQLQueryBuilder.TargetQueryBuilder.studyInstance().build().toString()).isEqualTo("study()");
    assertThat(RQLQueryBuilder.TargetQueryBuilder.datasetInstance().build().toString()).isEqualTo("dataset()");
    assertThat(RQLQueryBuilder.TargetQueryBuilder.variableInstance().build().toString()).isEqualTo("variable()");
  }

  @Test
  public void test_target_builder_sort() {
    assertThat(RQLQueryBuilder.TargetQueryBuilder.studyInstance().sort("name", null).build().toString())
      .isEqualTo("study(sort(+name))");
  }

  @Test
  public void test_target_builder_sort_asc() {
    assertThat(RQLQueryBuilder.TargetQueryBuilder.studyInstance().sort("name", "asc").build().toString())
      .isEqualTo("study(sort(+name))");
  }

  @Test
  public void test_target_builder_sort_desc() {
    assertThat(RQLQueryBuilder.TargetQueryBuilder.studyInstance().sort("name", "desc").build().toString())
      .isEqualTo("study(sort(-name))");
  }

  @Test
  public void test_target_builder_exists() {
    assertThat(RQLQueryBuilder.TargetQueryBuilder.studyInstance().exists("id").build().toString())
      .isEqualTo("study(exists(id))");
  }

  @Test
  public void test_target_builder_limit_sort_exists() {
    RQLQueryBuilder.TargetQueryBuilder studyQueryBuilder = RQLQueryBuilder.TargetQueryBuilder.studyInstance();
    assertThat(studyQueryBuilder.build().toString()).isEqualTo("study()");

    studyQueryBuilder.limit(0, 30);
    assertThat(studyQueryBuilder.build().toString()).isEqualTo("study(limit(0,30))");

    studyQueryBuilder.sort("name", "desc");
    assertThat(studyQueryBuilder.build().toString()).isEqualTo("study(limit(0,30),sort(-name))");

    studyQueryBuilder.exists("id");
    assertThat(studyQueryBuilder.build().toString()).isEqualTo("study(limit(0,30),sort(-name),exists(id))");
  }

  @Test
  public void test_query_builder() {
    assertThat(RQLQueryBuilder.newInstance().build().toString()).isEqualTo("()");
  }

  @Test
  public void test_query_builder_locale() {
    assertThat(RQLQueryBuilder.newInstance().locale("de").build().toString()).isEqualTo("(locale(de))");
  }

  @Test
  public void test_query_builder_study_target() {
    assertThat(RQLQueryBuilder.newInstance()
      .target(RQLQueryBuilder.TargetQueryBuilder.studyInstance().build())
      .locale("de").build().toString()).isEqualTo("(study(),locale(de))");
  }

  @Test
  public void test_query_builder_study_target_sort_limit_exists() {
    assertThat(buildSampleStudyQuery().build().toString())
      .isEqualTo("(study(exists(acronym),limit(0,50),sort(-acronym)),locale(de))");
  }

  @Test
  public void test_query_builder_study_target_sort_limit_exists_args() {
    assertThat(buildSampleStudyQuery().buildArgsAsString().toString())
      .isEqualTo("study(exists(acronym),limit(0,50),sort(-acronym)),locale(de)");
  }

  private RQLQueryBuilder buildSampleStudyQuery() {
    return RQLQueryBuilder.newInstance()
      .target(RQLQueryBuilder.TargetQueryBuilder.studyInstance()
        .exists("acronym")
        .limit(0, 50)
        .sort("acronym", "desc")
        .build())
      .locale("de");
  }
}
