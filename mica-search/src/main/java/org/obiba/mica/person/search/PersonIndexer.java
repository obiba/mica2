/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.person.search;

import javax.inject.Inject;

import org.obiba.mica.contact.event.PersonUpdatedEvent;
import org.obiba.mica.contact.event.IndexContactsEvent;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.core.repository.PersonRepository;
import org.obiba.mica.spi.search.Indexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;

@Component
public class PersonIndexer {

  private static final Logger log = LoggerFactory.getLogger(PersonIndexer.class);

  @Inject
  private PersonRepository personRepository;

  @Inject
  private Indexer indexer;

  @Async
  @Subscribe
  public void personUpdated(PersonUpdatedEvent event) {
    log.info("Person {} was updated", event.getPersistable());
    indexer.index(Indexer.PERSON_INDEX, event.getPersistable());
  }

  @Async
  @Subscribe
  public void reIndexContacts(IndexContactsEvent event) {
    log.info("Reindexing all persons");
    if(indexer.hasIndex(Indexer.PERSON_INDEX)) indexer.dropIndex(Indexer.PERSON_INDEX);

    Pageable pageRequest = new PageRequest(0, 100);
    Page<Person> persons;

    do {
      persons = personRepository.findAll(pageRequest);
      indexer.indexAll(Indexer.PERSON_INDEX, persons);
    } while((pageRequest = persons.nextPageable()) != null);
  }
}
