/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.web.model;


import com.google.common.collect.Sets;
import org.obiba.mica.core.domain.ComposedSet;
import org.obiba.mica.core.domain.DocumentSet;
import org.obiba.mica.core.domain.SetOperation;
import org.obiba.mica.dataset.service.VariableSetOperationService;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import java.util.Set;

@Component
class SetDtos {

  @Inject
  private VariableSetOperationService variableSetOperationService;

  Mica.DocumentSetDto asDto(DocumentSet documentSet) {
    Mica.DocumentSetDto.Builder builder = Mica.DocumentSetDto.newBuilder()
      .setId(documentSet.getId())
      .setType(documentSet.getType())
      .setTimestamps(TimestampsDtos.asDto(documentSet))
      .setCount(documentSet.getIdentifiers().size());

    if (documentSet.hasName()) builder.setName(documentSet.getName());
    if (documentSet.hasUsername()) builder.setUsername(documentSet.getUsername());
    builder.setLocked(documentSet.isLocked());

    return builder.build();
  }

  Mica.SetOperationDto asDto(SetOperation setOperation) {
    Set<String> operands = Sets.newLinkedHashSet();
    setOperation.getCompositions().forEach(set -> operands.addAll(set.getOperands()));
    Mica.SetOperationDto.Builder builder = Mica.SetOperationDto.newBuilder()
      .setId(setOperation.getId())
      .setType(setOperation.getType())
      .addAllSets(operands);
    setOperation.getCompositions().forEach(set -> builder.addCompositions(asDto(setOperation, set)));
    return builder.build();
  }

  Mica.ComposedSetDto asDto(SetOperation setOperation, ComposedSet composedSet) {
    Mica.ComposedSetDto.Builder builder = Mica.ComposedSetDto.newBuilder()
      .setId(composedSet.getId())
      .setType(setOperation.getType())
      .setTimestamps(TimestampsDtos.asDto(setOperation))
      .setName(composedSet.getName())
      .setOperation(setOperation.getId())
      .addAllSets(composedSet.getOperands())
      .setQuery(composedSet.getQuery());


    if (variableSetOperationService.isForType(setOperation))
      builder.setCount(variableSetOperationService.countDocuments(composedSet));
    else
      builder.setCount(0);

    if (setOperation.hasUsername()) builder.setUsername(setOperation.getUsername());

    return builder.build();
  }

}
