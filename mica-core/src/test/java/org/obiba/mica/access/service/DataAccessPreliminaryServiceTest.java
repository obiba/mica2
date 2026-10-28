/*
 * Copyright (c) 2026 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access.service;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.obiba.mica.access.DataAccessPreliminaryRepository;
import org.obiba.mica.access.domain.DataAccessPreliminary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The preliminaries of the listed data access requests are fetched in one query, only the missing ones are created.
 */
public class DataAccessPreliminaryServiceTest {

  @Mock
  private DataAccessPreliminaryRepository dataPreliminaryRequestRepository;

  @Spy
  @InjectMocks
  private DataAccessPreliminaryService service;

  private AutoCloseable mocks;

  @BeforeEach
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  public void tearDown() throws Exception {
    mocks.close();
  }

  @Test
  public void test_getOrCreate_all_existing_does_not_create() {
    List<String> ids = List.of("dar1", "dar2");
    when(dataPreliminaryRequestRepository.findAllById(ids)).thenReturn(List.of(preliminary("dar1"), preliminary("dar2")));

    Map<String, DataAccessPreliminary> found = service.getOrCreate(ids);

    assertThat(found).containsOnlyKeys("dar1", "dar2");
    assertThat(found.get("dar1").getParentId()).isEqualTo("dar1");
    verify(service, never()).getOrCreate(anyString());
  }

  @Test
  public void test_getOrCreate_creates_missing_only() {
    List<String> ids = List.of("dar1", "dar2");
    when(dataPreliminaryRequestRepository.findAllById(ids)).thenReturn(List.of(preliminary("dar1")));
    DataAccessPreliminary created = preliminary("dar2");
    doReturn(created).when(service).getOrCreate("dar2");

    Map<String, DataAccessPreliminary> found = service.getOrCreate(ids);

    assertThat(found).containsOnlyKeys("dar1", "dar2");
    assertThat(found.get("dar2")).isSameAs(created);
    verify(service, never()).getOrCreate("dar1");
  }

  @Test
  public void test_getOrCreate_no_ids() {
    when(dataPreliminaryRequestRepository.findAllById(List.of())).thenReturn(List.of());

    assertThat(service.getOrCreate(List.<String>of())).isEmpty();
  }

  private static DataAccessPreliminary preliminary(String id) {
    DataAccessPreliminary preliminary = new DataAccessPreliminary();
    preliminary.setId(id);
    preliminary.setParentId(id);
    return preliminary;
  }
}
