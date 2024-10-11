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
import org.obiba.mica.project.domain.Project;
import org.obiba.mica.spi.search.Indexer;
import org.springframework.stereotype.Component;

@Component
public class DraftProjectIndexer extends BaseIndexer<Project> {

  @Override
  public boolean isFor(String indexName) {
    return Indexer.DRAFT_PROJECT_INDEX.equals(indexName);
  }

  protected Document asDocument(Project project) {
    Document doc = new Document();
    doc.add(new StringField("_id", project.getId(), Field.Store.YES));
    doc.add(new StringField("_class", project.getClass().getSimpleName(), Field.Store.YES));

    StringBuilder content = new StringBuilder(project.getId());
    doc.add(new TextField("id", project.getId(), Field.Store.YES));
    content.append(" ").append(addLocalizedString(doc, "title", project.getTitle()));

    doc.add(new TextField("_content", content.toString(), Field.Store.NO));

    return doc;
  }
}
