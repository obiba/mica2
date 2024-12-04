package org.obiba.mica.security.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import org.obiba.mica.security.Roles;
import org.obiba.shiro.realm.GroupsToRolesMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MicaGroupsToRolesMapper implements GroupsToRolesMapper {

  private static final Logger log = LoggerFactory.getLogger(MicaGroupsToRolesMapper.class);

  private Map<String, List<Set<String>>> roleGroups = Maps.newHashMap();

  public MicaGroupsToRolesMapper(Environment environment) {
    Roles.ALL_ROLES.forEach(role -> {
      addRoleGroups(environment, role);
    });
  }

  @VisibleForTesting
  MicaGroupsToRolesMapper(String groupsStr, String someRole) {
    Roles.ALL_ROLES.forEach(role -> {
      if (role.equals(someRole)) {
        addRoleGroups(groupsStr, role);
      } else {
        addRoleGroups("", role);
      }
    });
  }


  @Override
  public Set<String> toRoles(Set<String> groups) {
    Set<String> roles = Roles.ALL_ROLES.stream()
      .filter(role -> hasRole(role, groups))
      .collect(Collectors.toSet());
    log.debug("roles: {}", Joiner.on(",").join(roles));
    roles.addAll(groups);

    return roles;
  }

  private void addRoleGroups(Environment environment, String role) {
    String groupsStr = environment.getProperty(String.format("roles.%s", role), role);
    addRoleGroups(groupsStr, role);
  }

  private void addRoleGroups(String groupsStr, String role) {
    Set<String> groupsCond = toSet(groupsStr, "\\|");
    List<Set<String>> groupsSets = groupsCond.stream()
      .map((cond) -> toSet(cond, ","))
      .toList();
    roleGroups.put(role, groupsSets);
  }

  private Set<String> toSet(String groupsStr, String separator) {
    return Arrays.stream(groupsStr.split(separator))
      .map(String::trim)
      .filter(s -> !s.isEmpty()) // Remove empty strings
      .collect(Collectors.toSet());
  }

  private boolean hasRole(String role, Set<String> groups) {
    if (!roleGroups.containsKey(role)) return false;
    for (Set<String> groupSet : roleGroups.get(role)) {
      if (groups.containsAll(groupSet)) return true;
    }
    return false;
  }
}
