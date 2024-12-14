/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.domain;

/**
 * Specify the access policy to the variable stats.
 */
public enum CatalogAccessPolicy {

  OPEN_ALL, // open access to catalog content

  OPEN_CATALOG, // open access to catalog content, require specific permissions for variable summary statistics

  RESTRICTED_ALL, // require specific permissions for catalog content and variable summary statistics

}
