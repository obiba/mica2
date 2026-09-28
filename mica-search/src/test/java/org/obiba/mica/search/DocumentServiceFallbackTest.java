/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search;

import org.junit.jupiter.api.Test;
import org.obiba.mica.core.service.DocumentService;
import org.obiba.mica.spi.search.Identified;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DocumentServiceFallbackTest {

  private static final List<String> DB_IDS = List.of("c", "a", "d", "b");

  static class DocService extends AbstractIdentifiedDocumentService<Identified> {
    boolean withFallback = true;

    @Override
    protected List<Identified> findFromDatabase(Collection<String> ids, String studyId) {
      if (!withFallback) return null;
      return DB_IDS.stream().filter(ids::contains).map(id -> (Identified) () -> id).collect(Collectors.toList());
    }

    @Override
    protected Identified processHit(Searcher.DocumentResult res) {
      return null;
    }

    @Override
    protected String getIndexName() {
      return "doc-draft";
    }

    @Override
    protected String getType() {
      return "Doc";
    }
  }

  /**
   * When the search engine fails (e.g. no shard available because of disk watermark), the drafts are listed
   * from the database, restricted to the accessible ids, sorted by id and paged.
   */
  @Test
  public void searchFailureFallsBackOnDatabase() {
    DocService service = newService(true);
    Searcher.IdFilter idFilter = () -> List.of("a", "b", "c");

    DocumentService.Documents<Identified> documents = service.find(1, 5, "id", "desc", null, null, null, null, idFilter);

    assertThat(documents.isDegraded()).isTrue();
    assertThat(documents.getTotal()).isEqualTo(3);
    assertThat(documents.getList().stream().map(Identified::getId)).containsExactly("b", "a");
  }

  @Test
  public void searchFailureIsRethrownWithoutFallback() {
    DocService service = newService(false);

    assertThatThrownBy(() -> service.find(0, 5, "id", "asc", null, null, null, null, () -> List.of("a")))
      .isInstanceOf(NullPointerException.class);
  }

  private DocService newService(boolean withFallback) {
    DocService service = new DocService();
    service.withFallback = withFallback;
    service.indexer = mock(Indexer.class);
    when(service.indexer.hasIndex("doc-draft")).thenReturn(true);
    // the ES8 plugin wraps a null response when the search fails
    Searcher.DocumentResults results = mock(Searcher.DocumentResults.class);
    when(results.getTotal()).thenThrow(new NullPointerException());
    service.searcher = mock(Searcher.class);
    when(service.searcher.getDocuments(any(), any(), any(Integer.class), any(Integer.class), any(), any(), any(), any(), any(), any(), any()))
      .thenReturn(results);
    return service;
  }
}
