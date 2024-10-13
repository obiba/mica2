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
import org.obiba.mica.file.Attachment;
import org.obiba.mica.file.AttachmentState;
import org.obiba.mica.spi.search.Indexer;
import org.springframework.stereotype.Component;

@Component
public class PublishedFileIndexer extends BaseIndexer<AttachmentState> {

  @Override
  public boolean isFor(String indexName) {
    return Indexer.ATTACHMENT_PUBLISHED_INDEX.equals(indexName);
  }

  protected Document asDocument(AttachmentState state, String parentId) {
    Document doc = new Document();
    doc.add(new StringField("_id", state.getId(), Field.Store.YES));
    doc.add(new StringField("_class", state.getClass().getSimpleName(), Field.Store.YES));

    Attachment attachment = state.getPublishedAttachment();
    if (attachment != null) {
      StringBuilder content = new StringBuilder(attachment.getId());
      doc.add(new TextField("id", attachment.getId(), Field.Store.NO));
      doc.add(new StringField("path", attachment.getPath(), Field.Store.YES));
      doc.add(new TextField("path.analyzed", attachment.getPath(), Field.Store.NO));
      doc.add(new StringField("name", attachment.getName(), Field.Store.YES));
      doc.add(new TextField("name.analyzed", attachment.getName(), Field.Store.NO));
      content.append(" ").append(attachment.getName());
      content.append(" ").append(addLocalizedString(doc, "description", attachment.getDescription()));

      doc.add(new TextField("_content", content.toString(), Field.Store.NO));
    }

    return doc;
  }
}
