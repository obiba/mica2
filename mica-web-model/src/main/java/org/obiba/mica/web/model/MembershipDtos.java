package org.obiba.mica.web.model;

import javax.inject.Inject;

import org.obiba.mica.core.domain.Address;
import org.obiba.mica.core.domain.Membership;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.domain.NetworkState;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.domain.StudyState;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.study.service.StudyService;
import org.springframework.stereotype.Component;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.stream.Collectors.toList;

@Component
@SuppressWarnings("OverlyCoupledClass")
class MembershipDtos {

  @Inject
  private PersonDtos personDtos;

  Mica.MembershipsDto asDto(Membership membership, boolean asDraft) {
    /*Mica.MembershipsDto.Builder builder = Mica.MembershipsDto.newBuilder();
    return builder.setRole(membership.getRole())
      .addAllMembers(personDtos.asDto(membership.getPerson(), asDraft)).build();*/
    throw new RuntimeException();
  }

  Membership fromDto(Mica.MembershipsDtoOrBuilder dto) {
    /*Membership membership = new Membership();
    membership.setRole(dto.getRole());
    membership.setPerson(personDtos.fromDto(dto.getPerson()));

    return membership;*/
    throw new RuntimeException();
  }
}
