/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import org.obiba.mica.core.domain.Address;
import org.obiba.mica.core.domain.EntityState;
import org.obiba.mica.core.domain.Person;
import org.obiba.mica.micaConfig.service.MicaConfigService;
import org.obiba.mica.network.domain.Network;
import org.obiba.mica.network.domain.NetworkState;
import org.obiba.mica.network.service.NetworkService;
import org.obiba.mica.network.service.PublishedNetworkService;
import org.obiba.mica.security.service.SubjectAclService;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.StudyState;
import org.obiba.mica.study.service.PublishedStudyService;
import org.obiba.mica.study.service.StudyService;
import org.springframework.stereotype.Component;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.stream.Collectors.toList;

@Component
@SuppressWarnings("OverlyCoupledClass")
class PersonDtos {

  @Inject
  private CountryDtos countryDtos;

  @Inject
  private PublishedStudyService publishedStudyService;

  @Inject
  private PublishedNetworkService publishedNetworkService;

  @Inject
  private NetworkService networkService;

  @Inject
  private LocalizedStringDtos localizedStringDtos;

  @Inject
  private SubjectAclService subjectAclService;

  @Inject
  private StudyService studyService;

  @Inject
  private MicaConfigService micaConfigService;

  Mica.PersonDto asDto(Person person, boolean asDraft) {
    Mica.PersonDto.Builder builder = Mica.PersonDto.newBuilder().setLastName(person.getLastName());
    if(!isNullOrEmpty(person.getId())) builder.setId(person.getId());
    if(!isNullOrEmpty(person.getTitle())) builder.setTitle(person.getTitle());
    if(!isNullOrEmpty(person.getFirstName())) builder.setFirstName(person.getFirstName());
    if(!isNullOrEmpty(person.getAcademicLevel())) builder.setAcademicLevel(person.getAcademicLevel());
    if(!isNullOrEmpty(person.getEmail())) builder.setEmail(person.getEmail());
    if(!isNullOrEmpty(person.getPhone())) builder.setPhone(person.getPhone());
    if(person.getInstitution() != null) builder.setInstitution(asDto(person.getInstitution()));

    List<String> roles = micaConfigService.getConfig().getRoles();

    builder.addAllStudyMemberships(person.getStudyMemberships().stream().filter(m -> {
      if(!roles.contains(m.getRole())) return false;

      EntityState state = studyService.findStateById(m.getParentId());
      if(asDraft) {
        return subjectAclService.isPermitted(
          state instanceof StudyState ? "/draft/individual-study" : "/draft/harmonization-study",
          "VIEW",
          m.getParentId());
      } else {
        return state != null &&
          state.isPublished() &&
          subjectAclService.isAccessible(state instanceof StudyState ? "/study" : "/harmonization-study", m.getParentId());
      }
    }).map(m -> asStudyMembershipDto(m, asDraft)).collect(toList()));
    builder.addAllNetworkMemberships(person.getNetworkMemberships().stream().filter(m -> {
      if(!roles.contains(m.getRole())) return false;

      if(asDraft) {
        return subjectAclService.isPermitted("/draft/network", "VIEW", m.getParentId());
      } else {
        NetworkState state = networkService.findStateById(m.getParentId());
        return state != null && state.isPublished() && subjectAclService.isAccessible("/network", m.getParentId());
      }
    }).map(m -> asNetworkMembershipDto(m, asDraft)).collect(toList()));

    return builder.build();
  }

  Person fromDto(Mica.PersonDtoOrBuilder dto) {
    Person person = new Person();
    if(dto.hasId()) person.setId(dto.getId());
    if(dto.hasTitle()) person.setTitle(dto.getTitle());
    if(dto.hasFirstName()) person.setFirstName(dto.getFirstName());
    person.setLastName(dto.getLastName());
    if(dto.hasAcademicLevel()) person.setAcademicLevel(dto.getAcademicLevel());
    if(dto.hasEmail()) person.setEmail(dto.getEmail());
    if(dto.hasPhone()) person.setPhone(dto.getPhone());
    if(dto.hasInstitution()) person.setInstitution(fromDto(dto.getInstitution()));

    if(dto.getNetworkMembershipsCount() > 0) {
      person.setNetworkMemberships(dto.getNetworkMembershipsList().stream().map(this::fromDto).collect(toList()));
    }

    if(dto.getStudyMembershipsCount() > 0) {
      person.setStudyMemberships(dto.getStudyMembershipsList().stream().map(this::fromDto).collect(toList()));
    }

    return person;
  }

  private Mica.PersonDto.InstitutionDto asDto(Person.Institution institution) {
    Mica.PersonDto.InstitutionDto.Builder builder = Mica.PersonDto.InstitutionDto.newBuilder();
    if(institution.getName() != null) builder.addAllName(localizedStringDtos.asDto(institution.getName()));
    if(institution.getDepartment() != null) {
      builder.addAllDepartment(localizedStringDtos.asDto(institution.getDepartment()));
    }
    if(institution.getAddress() != null) builder.setAddress(asDto(institution.getAddress()));
    return builder.build();
  }

  private Mica.PersonDto.MembershipDto asStudyMembershipDto(Person.Membership membership, boolean asDraft) {
    Mica.PersonDto.MembershipDto.Builder builder = Mica.PersonDto.MembershipDto.newBuilder();
    builder.setRole(membership.getRole());
    builder.setParentId(membership.getParentId());

    if(membership.getParentId() != null) {
      BaseStudy study = asDraft
        ? studyService.findStudy(membership.getParentId())
        : publishedStudyService.findById(membership.getParentId());

      if(study != null) {
        builder.addAllParentAcronym(localizedStringDtos.asDto(study.getAcronym()));
        builder.addAllParentName(localizedStringDtos.asDto(study.getName()));
        builder.setExtension(
          Mica.PersonDto.StudyMembershipDto.meta,
          Mica.PersonDto.StudyMembershipDto.newBuilder().setType(study.getResourcePath()).build()
        );
      }
    }

    return builder.build();
  }

  private Mica.PersonDto.MembershipDto asNetworkMembershipDto(Person.Membership membership, boolean asDraft) {
    Mica.PersonDto.MembershipDto.Builder builder = Mica.PersonDto.MembershipDto.newBuilder();
    builder.setRole(membership.getRole());
    builder.setParentId(membership.getParentId());

    if(membership.getParentId() != null) {
      Network network = asDraft
        ? networkService.findById(membership.getParentId())
        : publishedNetworkService.findById(membership.getParentId());
      if(network != null) {
        builder.addAllParentAcronym(localizedStringDtos.asDto(network.getAcronym()));
        builder.addAllParentName(localizedStringDtos.asDto(network.getName()));
      }
    }

    return builder.build();
  }

  private Person.Membership fromDto(Mica.PersonDto.MembershipDto dto) {
    return new Person.Membership(dto.getParentId(), dto.getRole());
  }

  private Person.Institution fromDto(Mica.PersonDto.InstitutionDtoOrBuilder dto) {
    Person.Institution institution = new Person.Institution();
    institution.setName(localizedStringDtos.fromDto(dto.getNameList()));
    institution.setDepartment(localizedStringDtos.fromDto(dto.getDepartmentList()));
    if(dto.hasAddress()) institution.setAddress(fromDto(dto.getAddress()));
    return institution;
  }

  private Mica.AddressDto asDto(Address address) {
    Mica.AddressDto.Builder builder = Mica.AddressDto.newBuilder();
    if(address.getStreet() != null) builder.addAllStreet(localizedStringDtos.asDto(address.getStreet()));
    if(address.getCity() != null) builder.addAllCity(localizedStringDtos.asDto(address.getCity()));
    if(!isNullOrEmpty(address.getZip())) builder.setZip(address.getZip());
    if(!isNullOrEmpty(address.getState())) builder.setState(address.getState());
    if(!isNullOrEmpty(address.getCountryIso())) builder.setCountry(countryDtos.asDto(address.getCountryIso()));
    return builder.build();
  }

  private Address fromDto(Mica.AddressDtoOrBuilder dto) {
    Address address = new Address();
    address.setStreet(localizedStringDtos.fromDto(dto.getStreetList()));
    address.setCity(localizedStringDtos.fromDto(dto.getCityList()));
    if(dto.hasZip()) address.setZip(dto.getZip());
    if(dto.hasState()) address.setState(dto.getState());
    if(dto.hasCountry()) address.setCountryIso(extractIso3CountryCode(dto.getCountry().getIso()));
    return address;
  }

  private String extractIso3CountryCode(String iso) {
    return iso != null && iso.length() == 2 ? new Locale("", iso).getISO3Country() : iso;
  }
}
