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
import org.obiba.mica.taxonomy.TaxonomyTermIndexable;
import org.obiba.opal.core.domain.taxonomy.Term;
import org.springframework.stereotype.Component;

@Component
public class DefaultTermIndexer extends BaseIndexer<TaxonomyTermIndexable> {

  @Override
  public boolean isFor(String indexName) {
    return Indexer.TERM_INDEX.equals(indexName);
  }

  protected Document asDocument(TaxonomyTermIndexable term, String parentId) {
    Document doc = new Document();
    doc.add(new StringField("_id", term.getId(), Field.Store.YES));
    doc.add(new StringField("_class", term.getClass().getSimpleName(), Field.Store.YES));

    StringBuilder content = new StringBuilder(term.getName());
    doc.add(new TextField("id", term.getId(), Field.Store.NO));
    doc.add(new TextField("name", term.getName(), Field.Store.NO));
    content.append(" ").append(addLocalizedString(doc, "title", term.getTitle()));
    content.append(" ").append(addLocalizedString(doc, "description", term.getDescription()));
    content.append(" ").append(addLocalizedString(doc, "keywords", term.getKeywords()));

    doc.add(new TextField("_content", content.toString(), Field.Store.NO));

    return doc;
  }
}
