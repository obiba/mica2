package org.obiba.mica.security.service;

import java.util.List;

import javax.inject.Inject;

import org.obiba.mica.security.domain.SubjectAcl;
import org.obiba.mica.security.event.SubjectAclUpdatedEvent;
import org.obiba.mica.security.repository.SubjectAclRepository;
import org.springframework.stereotype.Service;

import com.google.common.eventbus.EventBus;

@Service
public class SubjectAclService {

  @Inject
  private SubjectAclRepository subjectAclRepository;

  @Inject
  private EventBus eventBus;

  public List<SubjectAcl> find(String principal, SubjectAcl.Type type) {
    return subjectAclRepository.findByPrincipalAndType(principal, type);
  }

  public void addUserPermission(String name, String resource, String action, String instance) {
    List<SubjectAcl> acls = subjectAclRepository.findByPrincipalAndTypeAndResourceAndInstance(name, SubjectAcl.Type.USER,
      resource, instance);
    SubjectAcl acl;
    if (acls == null || acls.isEmpty()) {
      acl = SubjectAcl.newBuilder(name, SubjectAcl.Type.USER).resource(resource).action(action)
        .instance(instance).build();
      subjectAclRepository.save(acl);
    } else {
      acl = acls.get(0);
      acl.addAction(action);
    }
    subjectAclRepository.save(acl);
    eventBus.post(new SubjectAclUpdatedEvent(SubjectAcl.Type.USER.subjectFor(name)));
  }
}
