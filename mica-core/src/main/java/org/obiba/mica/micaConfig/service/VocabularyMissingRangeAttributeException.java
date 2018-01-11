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

public class VocabularyMissingRangeAttributeException extends AbstractVocabularyException {

  private static final long serialVersionUID = 3925364191872315281L;

  public VocabularyMissingRangeAttributeException() {
    super();
  }

  public VocabularyMissingRangeAttributeException(Vocabulary v) {
    super(String.format("The vocabulary '%s' with terms is missing a range attribute.", v.getName()), v);
  }
}
