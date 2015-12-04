package org.obiba.mica.security.repository;

import java.util.List;

import org.obiba.mica.security.domain.SubjectAcl;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface SubjectAclRepository extends MongoRepository<SubjectAcl, String> {

  List<SubjectAcl> findByPrincipalAndType(String principal, SubjectAcl.Type type);

  @Query("{'resource': ?0, 'instance': {$regex: ?1}}")
  List<SubjectAcl> findByResourceAndInstanceRegex(String resource, String instance);

  List<SubjectAcl> findByResourceAndInstance(String resource, String instance);

  List<SubjectAcl> findByResourceAndInstance(String resource, String instance, Sort sort);

  List<SubjectAcl> findByResourceStartingWith(String regex);

  List<SubjectAcl> findByPrincipalAndTypeAndResourceAndInstance(String principal, SubjectAcl.Type type, String resource, String instance);

}
