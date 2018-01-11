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

import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.PermissionResolver;
import org.springframework.stereotype.Component;

@Component
public class MicaPermissionResolver implements PermissionResolver {

  @Override
  public Permission resolvePermission(String permissionString) {
    return new ExtendedWildcardPermission(permissionString);
  }

}

