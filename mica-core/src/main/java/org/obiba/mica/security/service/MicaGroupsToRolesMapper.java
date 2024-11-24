package org.obiba.mica.security.service;

import com.google.common.collect.Maps;
import org.obiba.mica.security.Roles;
import org.obiba.shiro.realm.GroupsToRolesMapper;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MicaGroupsToRolesMapper implements GroupsToRolesMapper {

  private Map<String, Set<String>> roleGroups = Maps.newHashMap();

  public MicaGroupsToRolesMapper(Environment environment) {
    Roles.ALL_ROLES.forEach(role -> {
      addRoleGroups(environment, role);
    });
  }

  @Override
  public Set<String> toRoles(Set<String> groups) {
    Set<String> roles = Roles.ALL_ROLES.stream()
      .filter(role -> hasRole(role, groups))
      .collect(Collectors.toSet());

    roles.addAll(groups);

    return roles;
  }

  private void addRoleGroups(Environment environment, String role) {
    String groupsStr = environment.getProperty(String.format("roles.%s", role), role);
    Set<String> groups = Arrays.stream(groupsStr.split(","))
      .map(String::trim)
      .filter(s -> !s.isEmpty()) // Remove empty strings
      .collect(Collectors.toSet());
    roleGroups.put(role, groups);
  }

  private boolean hasRole(String role, Set<String> groups) {
    if (!roleGroups.containsKey(role)) return false;
    return groups.containsAll(roleGroups.get(role));
  }
}
