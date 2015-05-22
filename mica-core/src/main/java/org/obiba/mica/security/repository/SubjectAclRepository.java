package org.obiba.mica.security.repository;

import java.util.List;

import org.obiba.mica.security.domain.SubjectAcl;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SubjectAclRepository extends MongoRepository<SubjectAcl, String> {

  List<SubjectAcl> findByPrincipalAndType(String principal, SubjectAcl.Type type);

  List<SubjectAcl> findByPrincipalAndTypeAndResourceAndInstance(String principal, SubjectAcl.Type type, String resource, String instance);

}
