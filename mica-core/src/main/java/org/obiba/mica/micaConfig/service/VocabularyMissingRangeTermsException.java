/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.micaConfig.service;

import org.obiba.opal.core.domain.taxonomy.Vocabulary;

public class VocabularyMissingRangeTermsException extends AbstractVocabularyException {

  private static final long serialVersionUID = 2011996222183229317L;

  public VocabularyMissingRangeTermsException() {
    super();
  }

  public VocabularyMissingRangeTermsException(Vocabulary v) {
    super(String.format("The vocabulary '%s' having a range attribute is missing terms.", v.getName()), v);
  }
}
