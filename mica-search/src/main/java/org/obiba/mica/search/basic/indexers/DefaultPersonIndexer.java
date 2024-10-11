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

import com.google.common.base.Strings;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.spi.search.Indexer;
import org.springframework.stereotype.Component;

@Component
public class DefaultPersonIndexer extends BaseIndexer<Person> {

  @Override
  public boolean isFor(String indexName) {
    return Indexer.PERSON_INDEX.equals(indexName);
  }

  protected Document asDocument(Person person) {
    Document doc = new Document();
    doc.add(new StringField("_id", person.getId(), Field.Store.YES));
    doc.add(new StringField("_class", person.getClass().getSimpleName(), Field.Store.YES));

    doc.add(new TextField("id", person.getId(), Field.Store.YES));
    if (!Strings.isNullOrEmpty(person.getFirstName()))
      doc.add(new TextField("first-name", person.getFirstName(), Field.Store.YES));
    if (!Strings.isNullOrEmpty(person.getLastName()))
      doc.add(new TextField("last-name", person.getLastName(), Field.Store.YES));
    if (!Strings.isNullOrEmpty(person.getEmail()))
      doc.add(new TextField("email", person.getEmail(), Field.Store.YES));

    String content = String.format("%s %s %s", person.getFirstName(), person.getLastName(), person.getEmail());

    doc.add(new TextField("_content", content, Field.Store.NO));

    return doc;
  }
}
