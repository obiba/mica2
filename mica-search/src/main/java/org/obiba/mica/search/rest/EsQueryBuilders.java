/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.rest;

import java.io.IOException;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

/**
 *
 */
public class EsQueryBuilders {

  private EsQueryBuilders() {}

  public static class EsBoolTermsQueryBuilder {

    private final XContentBuilder builder;

    private EsBoolTermsQueryBuilder() throws IOException {
      builder = XContentFactory.jsonBuilder().startObject() //
          .startObject("query") //
          .startObject("bool") //
          .startArray("must");
    }

    public static EsBoolTermsQueryBuilder newBuilder() throws IOException {
      return new EsBoolTermsQueryBuilder();
    }

    public EsBoolTermsQueryBuilder addTerm(String name, String value) throws IOException {
      builder.startObject() //
          .startObject("term").field(name, value).endObject() //
          .endObject();
      return this;
    }

    public EsBoolTermsQueryBuilder addTerms(String name, String... values) throws IOException {
      builder.startObject() //
          .startObject("terms").array(name, values).endObject() //
          .endObject();
      return this;
    }

    public String build() throws IOException {
      builder.endArray() // must
          .endObject() // bool
          .endObject(); //query
      return builder.string();
    }

  }

}
