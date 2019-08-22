/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service.helper;

import java.io.Serializable;

import org.obiba.mica.core.domain.LocalizedString;

import com.google.common.base.Strings;

public interface AggregationMetaDataProvider {
  /**
   * Given a agregation name and a terms aggregation bucket key, returns the correspoding metadata
   * @param aggregation
   * @param termKey
   * @param locale
   * @return
   */
  MetaData getMetadata(String aggregation, String termKey, String locale);

  boolean containsAggregation(String aggregation);

  void refresh();

  class LocalizedMetaData implements Serializable {

    private static final long serialVersionUID = 6749141844859799977L;

    private LocalizedString title;
    private LocalizedString description;
    private String className;
    private String start;
    private String end;
    private String sortField;

    public LocalizedMetaData(LocalizedString title, LocalizedString description, String className) {
      this.title = title;
      this.description = description;
      this.className = className;
    }

    public LocalizedMetaData(LocalizedString title, LocalizedString description, String className, String start, String end) {
      this(title, description, className);
      this.start = start;
      this.end = end;
    }

    public LocalizedMetaData(LocalizedString title, LocalizedString description, String className, String start, String end, String sortField) {
      this(title, description, className, start, end);
      this.sortField = sortField;
    }

    public LocalizedString getTitle() {
      return title;
    }

    public LocalizedString getDescription() {
      return description;
    }

    public String getClassName() {
      return className;
    }

    public String getStart() {
      return start;
    }

    public String getEnd() {
      return end;
    }

    public String getSortField() {
      return sortField;
    }
  }

  class MetaData {
    private String title;
    private String description;
    private String className;
    private String start;
    private String end;
    private String sortField;

    private MetaData() {}

    public MetaData(String title, String description) {
      this.title = title;
      this.description = description;
    }

    public boolean hasTitle() {
      return !Strings.isNullOrEmpty(title);
    }

    public String getTitle() {
      return title;
    }

    public boolean hasDescription() {
      return !Strings.isNullOrEmpty(description);
    }

    public String getDescription() {
      return description;
    }

    public boolean hasClassName() {
      return !Strings.isNullOrEmpty(className);
    }

    public String getClassName() {
      return className;
    }

    public boolean hasStart() {
      return !Strings.isNullOrEmpty(start);
    }

    public String getStart() {
      return start;
    }

    public boolean hasEnd() {
      return !Strings.isNullOrEmpty(end);
    }

    public String getEnd() {
      return end;
    }

    public boolean hasSortField() {
      return !Strings.isNullOrEmpty(sortField);
    }

    public String getSortField() {
      return sortField;
    }

    public static Builder newBuilder() {
      return new Builder();
    }

    public static class Builder {

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

      public Builder className(String value) {
        metaData.className = value;
        return this;
      }

      public Builder start(String value) {
        metaData.start = value;
        return this;
      }

      public Builder end(String value) {
        metaData.end = value;
        return this;
      }

      public Builder sortField(String sortField) {
        metaData.sortField = sortField;
        return this;
      }

      public MetaData build() {
        return metaData;
      }
    }
  }
}
