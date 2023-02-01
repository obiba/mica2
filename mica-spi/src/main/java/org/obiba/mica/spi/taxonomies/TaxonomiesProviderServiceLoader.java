/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.spi.taxonomies;

import com.google.common.collect.Lists;

import java.net.URLClassLoader;
import java.util.Collection;
import java.util.ServiceLoader;

/**
 * {@link TaxonomiesProviderService} loader.
 */
public class TaxonomiesProviderServiceLoader {

  public static Collection<TaxonomiesProviderService> get(URLClassLoader classLoader) {
    return Lists.newArrayList(ServiceLoader.load(TaxonomiesProviderService.class, classLoader).iterator());
  }

}
