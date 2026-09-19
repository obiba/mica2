package org.obiba.mica.security.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import jakarta.inject.Inject;
import org.obiba.mica.security.Roles;
import org.obiba.shiro.realm.GroupsToRolesMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maps the groups a user belongs to (as reported by Agate) to the Mica built-in roles, according to
 * the {@code roles.<role>} settings, and conversely resolves the Agate groups that grant a role (for
 * instance to notify all the users having that role).
 */
@Component
public class MicaGroupsToRolesMapper implements GroupsToRolesMapper {

  private static final Logger log = LoggerFactory.getLogger(MicaGroupsToRolesMapper.class);

  private Map<String, List<Set<String>>> roleGroups = Maps.newHashMap();

  @Inject
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

  /**
   * Get the groups that grant the given role: all the groups appearing in any of the role's conditions
   * (when a condition requires several groups, the members of each of them are returned). When no
   * mapping is configured for the role, the role name is a group of its own.
   *
   * @param role
   * @return
   */
  public Set<String> toGroups(String role) {
    List<Set<String>> groupsSets = roleGroups.get(role);
    if (groupsSets == null || groupsSets.isEmpty()) return Sets.newHashSet(role);
    Set<String> groups = Sets.newLinkedHashSet();
    groupsSets.forEach(groups::addAll);
    return groups;
  }

  /**
   * Get the groups that grant any of the given roles.
   *
   * @param roles
   * @return
   */
  public Set<String> toGroups(String... roles) {
    Set<String> groups = Sets.newLinkedHashSet();
    Arrays.stream(roles).forEach(role -> groups.addAll(toGroups(role)));
    return groups;
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
