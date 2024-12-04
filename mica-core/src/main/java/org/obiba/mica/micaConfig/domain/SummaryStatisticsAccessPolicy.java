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
public enum SummaryStatisticsAccessPolicy {

  OPEN_ALL, // open basic counts, detailed stats and contingency

  OPEN_SUMMARY, // open basic counts and detailed stats, restricted contingency

  OPEN_BASICS, // open basic counts, restricted detailed stats and contingency

  RESTRICTED_ALL, // restricted basic counts, detailed stats and contingency

}
