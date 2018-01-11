/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.spi.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;

import java.util.List;

public interface ConfigurationProvider {

  List<String> getLocales();

  List<String> getRoles();

  ObjectMapper getObjectMapper();

  Taxonomy getNetworkTaxonomy();

  Taxonomy getStudyTaxonomy();

  Taxonomy getVariableTaxonomy();

  Taxonomy getDatasetTaxonomy();

  List<Taxonomy> getVariableTaxonomies();
}
