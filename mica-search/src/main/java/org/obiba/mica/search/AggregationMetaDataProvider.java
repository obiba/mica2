/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search;

public interface AggregationMetaDataProvider {
  /**
   * Given a agregation name and a terms aggregation bucket key, returns the correspoding metadata
   * @param aggregation
   * @param termKey
   * @param locale
   * @return
   */
  MetaData getTitle(String aggregation, String termKey, String locale);

  class MetaData {
    private String title;
    private String description;

    private MetaData() {}

    public String getTitle() {
      return title;
    }

    public String getDescription() {
      return description;
    }

    public static Builder newBuilder() {
      return new Builder();
    }

    static class Builder {

      private MetaData metaData = new MetaData();

      private Builder() {}

      public Builder title(String value) {
        metaData.title = value;
        return this;
      }

      public Builder description(String value) {
        metaData.description = value;
        return this;
      }

      public MetaData build() {
        return metaData;
      }

    }

  }
}
