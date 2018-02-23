/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core.service;


import com.google.common.base.Joiner;
import org.apache.shiro.SecurityUtils;
import org.joda.time.DateTime;
import org.obiba.mica.core.domain.ComposedSet;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.domain.SetOperation;
import org.obiba.mica.core.repository.SetOperationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public abstract class SetOperationService {

  private static final Logger log = LoggerFactory.getLogger(SetOperationService.class);

  @Inject
  private SetOperationRepository setOperationRepository;

  /**
   * Get the document type.
   *
   * @return
   */
  public abstract String getType();

  /**
   * Count the documents that can be retrieved from the RQL query of the set composition.
   *
   * @param composedSet
   * @return
   */
  public abstract long countDocuments(ComposedSet composedSet);

  /**
   * Get the set operation with ID and current type.
   *
   * @param operationId
   * @return
   */
  public SetOperation get(String operationId) {
    return setOperationRepository.findByTypeAndId(getType(), operationId);
  }

  /**
   * Create all possible sub-sets from the provided {@link DocumentSet} (maximum of 3) plus the union of all.
   *
   * @param sets
   * @return
   */
  public SetOperation create(List<DocumentSet> sets) {
    if (sets.isEmpty()) throw new IllegalArgumentException("Sets to compose are missing");
    if (sets.size() == 1) throw new IllegalArgumentException("Cannot compose a single set");
    if (sets.size() > 3) throw new IllegalArgumentException("Cannot compose more than 3 sets");

    for (DocumentSet set : sets) {
      if (!getType().equals(set.getType()))
        throw new IllegalArgumentException("Wrong set type: " + set.getType() + ", expecting " + getType());
    }

    SetOperation setOperation = new SetOperation();
    setOperation.setType(getType());
    setOperation.setLastModifiedDate(DateTime.now());
    Object principal = SecurityUtils.getSubject().getPrincipal();
    if (principal != null) {
      setOperation.setUsername(principal.toString());
    }

    List<String> operands = sets.stream().map(DocumentSet::getId).collect(Collectors.toList());
    setOperation.addComposition(createUnion(operands));
    setOperation.addComposition(createInter(operands));
    if (operands.size() == 2) {
      setOperation.addComposition(createDiff12(operands));
      setOperation.addComposition(createDiff21(operands));
    } else {
      setOperation.addComposition(createDiffInter123(operands));
      setOperation.addComposition(createDiffInter231(operands));
      setOperation.addComposition(createDiffInter312(operands));
      setOperation.addComposition(createDiffUnion123(operands));
      setOperation.addComposition(createDiffUnion231(operands));
      setOperation.addComposition(createDiffUnion312(operands));
    }

    return save(setOperation);
  }

  /**
   * Verify that the set operation applies to this service.
   *
   * @param setOperation
   * @return
   */
  public boolean isForType(SetOperation setOperation) {
    return setOperation != null && getType().equals(setOperation.getType());
  }

  //
  // Private methods
  //

  private SetOperation save(SetOperation setOperation) {
    return setOperationRepository.save(setOperation);
  }

  /**
   * in(sets,(S1,S2,S3))
   *
   * @param operands
   * @return
   */
  private ComposedSet createUnion(List<String> operands) {
    ComposedSet composedSet = newComposedSet(operands);
    String query = "in(sets,(" + Joiner.on(",").join(operands) + "))";
    composedSet.setQuery(query);
    composedSet.setName(Joiner.on(" &Union; ").join(operands.stream().map(op -> "S" + (operands.indexOf(op) + 1)).collect(Collectors.toList())));
    return composedSet;
  }

  /**
   * and(in(sets,S1),in(sets,S2),in(sets,S3))
   *
   * @param operands
   * @return
   */
  private ComposedSet createInter(List<String> operands) {
    ComposedSet composedSet = newComposedSet(operands);
    String query = "and(" + Joiner.on(",").join(operands.stream().map(op -> "in(sets," + op + ")").collect(Collectors.toList())) + ")";
    composedSet.setQuery(query);
    composedSet.setName(Joiner.on(" &Intersection; ").join(operands.stream().map(op -> "S" + (operands.indexOf(op) + 1)).collect(Collectors.toList())));
    return composedSet;
  }

  /**
   * and(in(sets,S1),out(sets,S2))
   *
   * @param operands
   * @return
   */
  private ComposedSet createDiff12(List<String> operands) {
    ComposedSet composedSet = newComposedSet(operands);
    String query = "and(in(sets," + operands.get(0) + "),out(sets," + operands.get(1) + "))";
    composedSet.setQuery(query);
    composedSet.setName("S1 - S2");
    return composedSet;
  }

  /**
   * and(in(sets,S2),out(sets,S1))
   *
   * @param operands
   * @return
   */
  private ComposedSet createDiff21(List<String> operands) {
    ComposedSet composedSet = newComposedSet(operands);
    String query = "and(in(sets," + operands.get(1) + "),out(sets," + operands.get(0) + "))";
    composedSet.setQuery(query);
    composedSet.setName("S2 - S1");
    return composedSet;
  }

  /**
   * and(in(sets,S1),in(sets,S2),out(sets,S3))
   *
   * @param operands
   * @return
   */
  private ComposedSet createDiffInter123(List<String> operands) {
    ComposedSet composedSet = newComposedSet(operands);
    setDiffInterQuery(composedSet, operands.get(0), operands.get(1), operands.get(2));
    composedSet.setName("(S1 &Intersection; S2) - S3");
    return composedSet;
  }

  /**
   * and(in(sets,S2),in(sets,S3),out(sets,S1))
   *
   * @param operands
   * @return
   */
  private ComposedSet createDiffInter231(List<String> operands) {
    ComposedSet composedSet = newComposedSet(operands);
    setDiffInterQuery(composedSet, operands.get(1), operands.get(2), operands.get(0));
    composedSet.setName("(S2 &Intersection; S3) - S1");
    return composedSet;
  }


  /**
   * and(in(sets,S3),in(sets,S1),out(sets,S2))
   *
   * @param operands
   * @return
   */
  private ComposedSet createDiffInter312(List<String> operands) {
    ComposedSet composedSet = newComposedSet(operands);
    setDiffInterQuery(composedSet, operands.get(2), operands.get(0), operands.get(1));
    composedSet.setName("(S3 &Intersection; S1) - S2");
    return composedSet;
  }

  private void setDiffInterQuery(ComposedSet composedSet, String op1, String op2, String op3) {
    String query = "and(in(sets," + op1 + "),in(sets," + op2 + "),out(sets," + op3 + "))";
    composedSet.setQuery(query);
  }

  /**
   * and(in(sets,S1),out(sets,(S2,S3)))
   *
   * @param operands
   * @return
   */
  private ComposedSet createDiffUnion123(List<String> operands) {
    ComposedSet composedSet = newComposedSet(operands);
    setDiffUnionQuery(composedSet, operands.get(0), operands.get(1), operands.get(2));
    composedSet.setName("S1 - (S2 &Union; S3)");
    return composedSet;
  }

  /**
   * and(in(sets,S2),out(sets,(S3,S1)))
   *
   * @param operands
   * @return
   */
  private ComposedSet createDiffUnion231(List<String> operands) {
    ComposedSet composedSet = newComposedSet(operands);
    setDiffUnionQuery(composedSet, operands.get(1), operands.get(2), operands.get(0));
    composedSet.setName("S2 - (S3 &Union; S1)");
    return composedSet;
  }

  /**
   * and(in(sets,S3),out(sets,(S1,S2)))
   *
   * @param operands
   * @return
   */
  private ComposedSet createDiffUnion312(List<String> operands) {
    ComposedSet composedSet = newComposedSet(operands);
    setDiffUnionQuery(composedSet, operands.get(2), operands.get(0), operands.get(1));
    composedSet.setName("S3 - (S1 &Union; S2)");
    return composedSet;
  }

  private void setDiffUnionQuery(ComposedSet composedSet, String op1, String op2, String op3) {
    String query = "and(in(sets," + op1 + "),out(sets,(" + op2 + "," + op3 + ")))";
    composedSet.setQuery(query);
  }

  private ComposedSet newComposedSet(List<String> operands) {
    ComposedSet composedSet = new ComposedSet();
    composedSet.setOperands(operands);
    return composedSet;
  }

}
