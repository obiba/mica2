/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.access.service;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.obiba.mica.access.domain.DataAccessAgreement;
import org.obiba.mica.access.domain.DataAccessAmendment;
import org.obiba.mica.access.domain.DataAccessEntity;
import org.obiba.mica.access.domain.DataAccessFeasibility;
import org.obiba.mica.access.domain.DataAccessPreliminary;
import org.obiba.mica.access.domain.DataAccessRequest;
import org.obiba.mica.micaConfig.domain.DataAccessConfig;
import org.obiba.mica.micaConfig.event.DataAccessConfigUpdatedEvent;
import org.obiba.mica.security.Roles;
import org.obiba.mica.security.service.SubjectAclService;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DataAccessDaoPermissionServiceTest {

  @Mock
  private SubjectAclService subjectAclService;

  @Mock
  private DataAccessRequestService dataAccessRequestService;

  @Mock
  private DataAccessFeasibilityService dataAccessFeasibilityService;

  @Mock
  private DataAccessAmendmentService dataAccessAmendmentService;

  @Mock
  private DataAccessPreliminaryService dataAccessPreliminaryService;

  @Mock
  private DataAccessAgreementService dataAccessAgreementService;

  private DataAccessDaoPermissionService service;

  @BeforeEach
  public void setUp() {
    service = new DataAccessDaoPermissionService(subjectAclService, dataAccessRequestService,
      dataAccessFeasibilityService, dataAccessAmendmentService, dataAccessPreliminaryService, dataAccessAgreementService);
    when(dataAccessRequestService.findByStatus(anyList())).thenReturn(List.of(withId(DataAccessRequest.newBuilder().build(), "dar1")));
    when(dataAccessFeasibilityService.findByStatus(anyList())).thenReturn(List.of(withId(DataAccessFeasibility.newBuilder().parentId("dar1").build(), "dar1-F1")));
    when(dataAccessAmendmentService.findByStatus(anyList())).thenReturn(List.of(withId(DataAccessAmendment.newBuilder().parentId("dar2").build(), "dar2-A1")));
    when(dataAccessPreliminaryService.findByStatus(anyList())).thenReturn(List.of(withId(DataAccessPreliminary.newBuilder().parentId("dar3").build(), "dar3")));
    when(dataAccessAgreementService.findByStatus(anyList())).thenReturn(List.of(withId(DataAccessAgreement.newBuilder().parentId("dar4").build(), "dar4-bob")));
  }

  @Test
  public void test_dao_can_edit_grants_edit_on_each_entity_under_its_own_parent() {
    service.onDataAccessConfigUpdated(event(true));

    verify(subjectAclService).addGroupPermission(Roles.MICA_DAO, "/data-access-request", "EDIT", "dar1");
    verify(subjectAclService).addGroupPermission(Roles.MICA_DAO, "/data-access-request/dar1/feasibility", "EDIT", "dar1-F1");
    verify(subjectAclService).addGroupPermission(Roles.MICA_DAO, "/data-access-request/dar2/amendment", "EDIT", "dar2-A1");
    verify(subjectAclService).addGroupPermission(Roles.MICA_DAO, "/data-access-request/dar3/preliminary", "EDIT", "dar3");
    verify(subjectAclService).addGroupPermission(Roles.MICA_DAO, "/data-access-request/dar4/agreement", "EDIT", "dar4-bob");
    verify(subjectAclService, never()).removeGroupPermission(anyString(), anyString(), anyString(), anyString());
  }

  @Test
  public void test_dao_cannot_edit_revokes_edit_on_each_entity_under_its_own_parent() {
    service.onDataAccessConfigUpdated(event(false));

    verify(subjectAclService).removeGroupPermission(Roles.MICA_DAO, "/data-access-request", "EDIT", "dar1");
    verify(subjectAclService).removeGroupPermission(Roles.MICA_DAO, "/data-access-request/dar1/feasibility", "EDIT", "dar1-F1");
    verify(subjectAclService).removeGroupPermission(Roles.MICA_DAO, "/data-access-request/dar2/amendment", "EDIT", "dar2-A1");
    verify(subjectAclService).removeGroupPermission(Roles.MICA_DAO, "/data-access-request/dar3/preliminary", "EDIT", "dar3");
    verify(subjectAclService).removeGroupPermission(Roles.MICA_DAO, "/data-access-request/dar4/agreement", "EDIT", "dar4-bob");
    verify(subjectAclService, never()).addGroupPermission(anyString(), anyString(), anyString(), anyString());
  }

  @SuppressWarnings("unchecked")
  private static <T extends DataAccessEntity> T withId(DataAccessEntity entity, String id) {
    entity.setId(id);
    return (T) entity;
  }

  private static DataAccessConfigUpdatedEvent event(boolean daoCanEdit) {
    DataAccessConfig config = new DataAccessConfig();
    config.setDaoCanEdit(daoCanEdit);
    return new DataAccessConfigUpdatedEvent(config);
  }
}
