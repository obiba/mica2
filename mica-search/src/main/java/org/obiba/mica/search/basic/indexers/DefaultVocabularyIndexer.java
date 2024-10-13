/*
 * Copyright (c) 2024 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.basic.indexers;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.taxonomy.TaxonomyVocabularyIndexable;
import org.springframework.stereotype.Component;

@Component
public class DefaultVocabularyIndexer extends BaseIndexer<TaxonomyVocabularyIndexable> {

  @Override
  public boolean isFor(String indexName) {
    return Indexer.VOCABULARY_INDEX.equals(indexName);
  }

  protected Document asDocument(TaxonomyVocabularyIndexable vocabulary, String parentId) {
    Document doc = new Document();
    doc.add(new StringField("_id", vocabulary.getId(), Field.Store.YES));
    doc.add(new StringField("_class", vocabulary.getClass().getSimpleName(), Field.Store.YES));

    StringBuilder content = new StringBuilder(vocabulary.getName());
    doc.add(new TextField("id", vocabulary.getId(), Field.Store.NO));
    doc.add(new TextField("name", vocabulary.getName(), Field.Store.NO));
    content.append(" ").append(addLocalizedString(doc, "title", vocabulary.getTitle()));
    content.append(" ").append(addLocalizedString(doc, "description", vocabulary.getDescription()));
    content.append(" ").append(addLocalizedString(doc, "keywords", vocabulary.getKeywords()));

    doc.add(new TextField("_content", content.toString(), Field.Store.NO));

    return doc;
  }
}
