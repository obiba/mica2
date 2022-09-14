/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.spi.search.rql;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
import org.obiba.mica.spi.search.IndexFieldMapping;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.TaxonomyEntity;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;

import com.google.common.collect.Lists;

public class RQLFieldResolver {

  public static final String TYPE_STRING = "string";
  public static final String TYPE_INTEGER = "integer";
  public static final String TYPE_DECIMAL = "decimal";

  private static final String TAXO_SEPARATOR = ".";

  private static final String DEFAULT_TAXO_PREFIX = "Mica_";

  private static final String POSFIX_ANALYZED = ".analyzed";

  private static final String LANGUAGE_TAG_UNDETERMINED = "und";

  private final List<Taxonomy> taxonomies;

  private final String defaultTaxonomyName;

  private final String locale;

  private final IndexFieldMapping indexFieldMapping;

  private final RQLNode node;

  private final Map<RQLNode, List<String>> nodeLocalizedFields = new HashMap<RQLNode, List<String>>() {
    {
      put(RQLNode.DATASET, Lists.newArrayList(Indexer.DATASET_LOCALIZED_ANALYZED_FIELDS));
      put(RQLNode.STUDY, Lists.newArrayList(Indexer.STUDY_LOCALIZED_ANALYZED_FIELDS));
      put(RQLNode.NETWORK, Lists.newArrayList(Indexer.NETWORK_LOCALIZED_ANALYZED_FIELDS));
      put(RQLNode.VARIABLE, Lists.newArrayList(Indexer.VARIABLE_LOCALIZED_ANALYZED_FIELDS));
    }
  };

  private final Map<RQLNode, List<String>> nodeAnalyzedFields = new HashMap<RQLNode, List<String>>() {
    {
      put(RQLNode.DATASET, Lists.newArrayList(Indexer.ANALYZED_FIELDS));
      put(RQLNode.STUDY, Lists.newArrayList(Indexer.ANALYZED_FIELDS));
      put(RQLNode.NETWORK, Lists.newArrayList(Indexer.ANALYZED_FIELDS));
      put(RQLNode.VARIABLE, Lists.newArrayList(Indexer.VARIABLE_ANALYZED_FIELDS));
    }
  };

  public RQLFieldResolver(RQLNode node, List<Taxonomy> taxonomies, String locale, IndexFieldMapping indexFieldMapping) {
    this.node = node;
    this.taxonomies = taxonomies;
    this.locale = locale;
    this.indexFieldMapping = indexFieldMapping;
    defaultTaxonomyName = taxonomies.stream().filter(t -> t.getName().startsWith(DEFAULT_TAXO_PREFIX))
      .map(TaxonomyEntity::getName).findFirst().orElse("");
  }

  public FieldData resolveField(String rqlField) {
    return resolveFieldAsBuilder(rqlField).build();
  }

  public FieldData resolveFieldUnanalyzed(String rqlField) {
    return resolveFieldAsBuilder(rqlField).unanalyze().build();
  }

  private FieldData.Builder resolveFieldAsBuilder(String rqlField) {
    String field = rqlField;
    if (isRegExp(field)) {
      // do not alter the regexp
      return FieldData.newBuilder().field(field);
    }

    // normalize field name
    if(!field.contains(TAXO_SEPARATOR)) {
      field = defaultTaxonomyName + TAXO_SEPARATOR + field;
    }

    int idx = field.indexOf(TAXO_SEPARATOR);
    if(idx < 1) return FieldData.newBuilder().field(rqlField);

    FieldData.Builder builder = resolveFieldInternal(field.substring(0, idx), field.substring(idx + 1, field.length()));

    return builder == null ? FieldData.newBuilder().field(rqlField) : builder;
  }

  private FieldData.Builder resolveFieldInternal(String taxonomyName, String vocabularyName) {
    String field = null;
    FieldData.Builder builder = FieldData.newBuilder();
    Optional<Taxonomy> taxonomy = taxonomies.stream().filter(t -> t.getName().equals(taxonomyName)).findFirst();
    if(taxonomy.isPresent() && taxonomy.get().hasVocabularies()) {
      builder.taxonomy(taxonomy.get());
      Optional<Vocabulary> vocabulary = taxonomy.get().getVocabularies().stream()
        .filter(v -> v.getName().equals(vocabularyName)).findFirst();
      if(vocabulary.isPresent()) {
        builder.vocabulary(vocabulary.get());
        String f = vocabulary.get().getAttributeValue("field");
        if(!Strings.isNullOrEmpty(f)) field = localize(vocabulary.get(), f, locale);
        else field = localize(vocabulary.get(), vocabulary.get().getName(), locale);
      } else {
        field = localize(null, vocabularyName, locale);
      }
    }
    return field == null ? null : builder.field(field);
  }

  public List<Taxonomy> getTaxonomies() {
    return taxonomies;
  }

  public List<String> getAnalzedFields() {
    return nodeAnalyzedFields.getOrDefault(node, Lists.newArrayList(Indexer.ANALYZED_FIELDS));
  }

  private String localize(Vocabulary vocabulary, String field, String locale) {
    boolean process = true;
    boolean localized = false;

    if (vocabulary != null) {
      localized = Boolean.valueOf(vocabulary.getAttributeValue("localized"));
      process = localized || !vocabulary.hasTerms() && new VocabularyWrapper(vocabulary).isString();
    }

    if (process) {
      Pattern pattern = Pattern.compile("\\." + LANGUAGE_TAG_UNDETERMINED + "$");
      Matcher matcher = pattern.matcher(field);

      field = matcher.find()
        ? field.replace(LANGUAGE_TAG_UNDETERMINED, locale)
        : localized ? field + TAXO_SEPARATOR + locale : getSafeLocalizedField(field);

      return indexFieldMapping.isAnalyzed(field) ? field + POSFIX_ANALYZED : field;
    }

    return field;
  }

  private String getSafeLocalizedField(String field) {
    return nodeLocalizedFields.getOrDefault(node, Lists.newArrayList()).contains(field) ? String.format("%s.%s", field, locale) : field;
  }

  private boolean isRegExp(String field) {
    Pattern pattern = Pattern.compile("\\^|\\$|\\*|\\\\|\\|");
    Matcher matcher = pattern.matcher(field);
    return matcher.find();
  }

  static class VocabularyWrapper {
    private Vocabulary vocabulary;

    VocabularyWrapper() {
    }

    VocabularyWrapper(Vocabulary vocabulary) {
      this.vocabulary = vocabulary;
    }

    public Vocabulary getVocabulary() {
      return vocabulary;
    }

    public String getType() {
      if (vocabulary == null) return TYPE_STRING;
      String type = vocabulary.getAttributeValue("type");
      return Strings.isNullOrEmpty(type)? TYPE_STRING : type.toLowerCase();
    }

    public boolean isNumeric() {
      String type = getType();
      return TYPE_INTEGER.equals(type) || TYPE_DECIMAL.equals(type);
    }

    public boolean isRange() {
      return vocabulary != null && vocabulary.hasTerms() && isNumeric();
    }

    public boolean isString() {
      return TYPE_STRING.equals(getType());
    }
  }

  public static class FieldData {
    private Taxonomy taxonomy;
    private VocabularyWrapper vocabulary = new VocabularyWrapper();
    private String field;

    static Builder newBuilder() {
      return new Builder();
    }

    public Taxonomy getTaxonomy() {
      return taxonomy;
    }

    public Vocabulary getVocabulary() {
      return vocabulary.getVocabulary();
    }

    public String getField() {
      return field;
    }

    public boolean isNumeric() {
      return vocabulary.isNumeric();
    }

    public boolean isRange() {
      return vocabulary.isRange();
    }

    static class Builder {
      FieldData data = new FieldData();

      Builder taxonomy(Taxonomy value) {
        data.taxonomy = value;
        return this;
      }

      Builder vocabulary(Vocabulary value) {
        data.vocabulary = new VocabularyWrapper(value);
        return this;
      }

      Builder field(String value) {
        data.field = value;
        return this;
      }

      Builder unanalyze() {
        data.field = Strings.isNullOrEmpty(data.field) ? data.field : data.field.replaceFirst("\\" + POSFIX_ANALYZED,"");
        return this;
      }

      FieldData build() {
        return data;
      }
    }
  }
}
