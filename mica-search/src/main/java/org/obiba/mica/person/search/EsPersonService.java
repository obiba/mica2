/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.person.search;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import org.elasticsearch.search.SearchHit;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.search.AbstractPublishedDocumentService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EsPersonService extends AbstractPublishedDocumentService<Person> {

  @Inject
  private ObjectMapper objectMapper;

  @Override
  protected Person processHit(SearchHit hit) throws IOException {
    InputStream inputStream = new ByteArrayInputStream(hit.getSourceAsString().getBytes());
    return objectMapper.readValue(inputStream, Person.class);
  }

  @Override
  protected String getIndexName() {
    return PersonIndexer.PERSON_INDEX;
  }

  @Override
  protected String getType() {
    return PersonIndexer.PERSON_TYPE;
  }
}
