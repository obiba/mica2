/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
 *
 *  This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.taxonomy;

import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class TaxonomyResolver {

  public static final String ID_SEPARATOR = ":";

  private String taxonomyName;

  private String vocabularyName;

  private String termName;

  public String getTaxonomyName() {
    return taxonomyName;
  }

  public boolean hasVocabularyName() {
    return !Strings.isNullOrEmpty(vocabularyName);
  }

  public String getVocabularyName() {
    return vocabularyName;
  }

  public boolean hasTermName() {
    return !Strings.isNullOrEmpty(termName);
  }

  public String getTermName() {
    return termName;
  }

  public static TaxonomyResolver fromId(String id) {
    String[] names = id.split(ID_SEPARATOR);
    TaxonomyResolver resolver = new TaxonomyResolver();
    resolver.taxonomyName = names[0];
    if(names.length > 1) resolver.vocabularyName = names[1];
    if(names.length > 2) resolver.termName = names[2];
    return resolver;
  }

  public static String asId(String taxonomyName, String vocabularyName, String termName) {
    return asId(taxonomyName, vocabularyName) + ID_SEPARATOR + termName;
  }

  public static String asId(String taxonomyName, String vocabularyName) {
    return taxonomyName + ID_SEPARATOR + vocabularyName;
  }

  public static Map<String, List<String>> asMap(List<String> ids) {
    Map<String, List<String>> vocabularyNames = Maps.newHashMap();
    ids.forEach(id -> {
      TaxonomyResolver resolver = fromId(id);
      if(!vocabularyNames.containsKey(resolver.getTaxonomyName()))
        vocabularyNames.put(resolver.getTaxonomyName(), Lists.newArrayList());
      if(!vocabularyNames.get(resolver.getTaxonomyName()).contains(resolver.getVocabularyName()))
        vocabularyNames.get(resolver.getTaxonomyName()).add(resolver.getVocabularyName());
    });
    return vocabularyNames;
  }
}
