/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.contact.search;

import javax.inject.Inject;

import org.obiba.mica.contact.event.ContactUpdatedEvent;
import org.obiba.mica.contact.event.IndexContactsEvent;
import org.obiba.mica.core.domain.Contact;
import org.obiba.mica.core.service.ContactService;
import org.obiba.mica.search.ElasticSearchIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;

@Component
public class ContactIndexer {

  private static final Logger log = LoggerFactory.getLogger(ContactIndexer.class);

  public static final String CONTACT_INDEX = "contact";

  public static final String CONTACT_TYPE = "Contact";

  @Inject
  private ContactService contactService;

  @Inject
  private ElasticSearchIndexer elasticSearchIndexer;

  @Async
  @Subscribe
  public void contactUpdated(ContactUpdatedEvent event) {
    log.info("Contact {} was updated", event.getPersistable());
    elasticSearchIndexer.index(CONTACT_INDEX, event.getPersistable());
  }

  @Async
  @Subscribe
  public void reIndexContacts(IndexContactsEvent event) {
    log.info("Reindexing all contacts");
    reIndexAll(CONTACT_INDEX, contactService.findAllContacts());
  }

  private void reIndexAll(String indexName, Iterable<Contact> contacts) {
    if(elasticSearchIndexer.hasIndex(indexName)) elasticSearchIndexer.dropIndex(indexName);
    elasticSearchIndexer.indexAll(indexName, contacts);
  }
}
