/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.security.realm;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.obiba.mica.file.FileUtils;

import com.google.common.collect.Lists;

/**
 * If the permission's instance value is a path, some derived permissions will be evaluated: parent path should be
 * viewable and children path should be applied the same actions.
 *
 * @see WildcardPermission
 */
public class ExtendedWildcardPermission extends WildcardPermission {

  private static final long serialVersionUID = -3283518923013288131L;

  private final List<WildcardPermission> derivedPermissions = Lists.newArrayList();

  public ExtendedWildcardPermission(String wildcardString) {
    this(wildcardString, true);
  }

  private ExtendedWildcardPermission(String wildcardString, boolean derive) {
    super(wildcardString);
    if(derive) initDerivedPermissions(wildcardString);
  }

  /**
   * If instance is a path, instances at parent paths are viewable.
   *
   * @param wildcardString
   */
  private void initDerivedPermissions(String wildcardString) {
    String[] parts = wildcardString.split(PART_DIVIDER_TOKEN);
    if(parts.length == 3) {
      String[] subParts = parts[2].split(SUBPART_DIVIDER_TOKEN);
      Arrays.stream(subParts).filter(sp -> sp.startsWith("/")).forEach(sp -> {
        // children path instances with wildcard
        derivedPermissions.add(
          new ExtendedWildcardPermission(parts[0] + PART_DIVIDER_TOKEN + parts[1] + PART_DIVIDER_TOKEN + sp + "/*",
            false));
        String path = sp;
        // parent path instances
        while(!FileUtils.isRoot(path)) {
          path = FileUtils.getParentPath(path);
          derivedPermissions
            .add(new WildcardPermission(parts[0] + PART_DIVIDER_TOKEN + "VIEW" + PART_DIVIDER_TOKEN + path));
        }
      });
    }
  }

  @Override
  public boolean implies(Permission p) {
    boolean rval = impliesInternal(p);
    if(rval) return true;

    for(WildcardPermission derived : derivedPermissions) {
      if(derived.implies(p)) return true;
    }

    return false;
  }

  private boolean impliesInternal(Permission p) {
    // By default only supports comparisons with other WildcardPermissions
    if(!(p instanceof ExtendedWildcardPermission)) {
      return false;
    }

    ExtendedWildcardPermission wp = (ExtendedWildcardPermission) p;

    List<Set<String>> otherParts = wp.getParts();

    int i = 0;
    for(Set<String> otherPart : otherParts) {
      // If this permission has less parts than the other permission, everything after the number of parts contained
      // in this permission is automatically implied, so return true
      if(getParts().size() - 1 < i) {
        return true;
      } else {
        Set<String> part = getParts().get(i);
        if(!part.contains(WILDCARD_TOKEN) && !matchesAll(part, otherPart)) {
          return false;
        }
        i++;
      }
    }

    // If this permission has more parts than the other parts, only imply it if all of the other parts are wildcards
    for(; i < getParts().size(); i++) {
      Set<String> part = getParts().get(i);
      if(!part.contains(WILDCARD_TOKEN)) {
        return false;
      }
    }

    return true;
  }

  private boolean matchesAll(Set<String> part, Set<String> otherPart) {
    for(String p : otherPart)
      if(!matches(part, p)) return false;
    return true;
  }

  private boolean matches(Set<String> part, @NotNull String str) {
    for(String p : part) {
      // detect children path wildcard
      if(p.startsWith("/") && p.endsWith("/*")) {
        if(str.startsWith(p.substring(0, p.length() - 1))) return true;
      } else if(p.equals(str)) return true;
    }
    return false;
  }
}
