/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest.entity.rql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import net.jazdw.rql.parser.ASTNode;
import net.jazdw.rql.parser.RQLParser;
import org.joda.time.DateTime;
import org.obiba.magma.NoSuchVariableException;
import org.obiba.magma.ValueType;
import org.obiba.magma.support.VariableNature;
import org.obiba.magma.type.TextType;
import org.obiba.mica.core.domain.BaseStudyTable;
import org.obiba.mica.dataset.domain.DatasetVariable;
import org.obiba.mica.dataset.domain.HarmonizationDataset;
import org.obiba.mica.dataset.domain.StudyDataset;
import org.obiba.mica.dataset.service.CollectedDatasetService;
import org.obiba.mica.dataset.service.HarmonizedDatasetService;
import org.obiba.mica.spi.search.Indexer;
import org.obiba.mica.spi.search.Searcher;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.mica.study.domain.HarmonizationStudy;
import org.obiba.mica.study.service.StudyService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Converts from RQL query from Mica to Opal.
 */
@Component
@Scope("prototype")
public class RQLCriteriaOpalConverter {

  private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  @Inject
  private CollectedDatasetService collectedDatasetService;

  @Inject
  private HarmonizedDatasetService harmonizedDatasetService;

  @Inject
  private StudyService studyService;

  @Inject
  private Searcher searcher;

  @Inject
  private ObjectMapper objectMapper;

  /**
   * RQL criterion list.
   */
  private List<RQLCriterionOpalConverter> criterionConverters = Lists.newArrayList();

  public RQLCriteriaOpalConverter() {
  }

  public void parse(String rqlQuery) {
    RQLParser parser = new RQLParser();
    ASTNode queryNode = parser.parse(rqlQuery);
    parseNode(queryNode);
  }

  public List<RQLCriterionOpalConverter> getCriterionConverters() {
    return criterionConverters;
  }

  ValueType getValueType() {
    return TextType.get();
  }

  VariableNature getNature() {
    return VariableNature.UNDETERMINED;
  }

  protected String join(String on, Collection<?> args) {
    String nOn = on;
    boolean toQuote = getNature().equals(VariableNature.CATEGORICAL);
    List<String> nArgs = args.stream().map(arg -> {
      String nArg = arg instanceof DateTime ? normalizeDate((DateTime) arg) : arg.toString();
      if (toQuote) return quote(nArg);
      else return normalizeString(nArg);
    }).collect(Collectors.toList());

    return Joiner.on(nOn).join(nArgs);
  }

  //
  // Private methods
  //

  private String toString(ASTNode node) {
    StringBuilder builder = new StringBuilder(node.getName()).append("(");
    for (int i = 0; i < node.getArgumentsSize(); i++) {
      if (i > 0) builder.append(",");
      append(builder, node.getArgument(i));
    }
    builder.append(")");
    return builder.toString();
  }

  private void append(StringBuilder builder, Object arg) {
    if (arg instanceof ASTNode) builder.append(toString((ASTNode) arg));
    else if (arg instanceof Collection) {
      builder.append("(");
      Collection<?> values = (Collection) arg;
      int i = 0;
      for (Object value : values) {
        if (i > 0) builder.append(",");
        append(builder, value);
        i++;
      }
      builder.append(")");
    } else if (arg instanceof DateTime) builder.append(normalizeDate((DateTime) arg));
    else builder.append(arg);
  }

  private void parseNode(ASTNode node) {
    if (Strings.isNullOrEmpty(node.getName()))
      parseNodes(node.getArguments());
    else
      parseNode(node, false);
  }

  private void parseNode(ASTNode node, boolean not) {
    if (node.getName().equals("not")) {
      parseNode((ASTNode) node.getArgument(0), true);
      return;
    }
    RQLFieldReferences references = parseField(node.getArgument(0).toString());

    switch (node.getName()) {
      case "exists":
      case "all":
        criterionConverters.add(RQLCriterionOpalConverter.newBuilder(node)
          .references(references).not(not).build());
        break;
      case "in":
      case "like":
        criterionConverters.add(RQLCriterionOpalConverter.newBuilder(node)
          .references(references).value(parseValue(node.getArgument(1))).not(not).build());
        break;
      case "range":
        criterionConverters.add(RQLCriterionOpalConverter.newBuilder(node)
          .references(references).value(parseRange(node.getArgument(1))).not(not).build());
        break;
    }
  }

  private RQLFieldReferences parseField(String path) {
    DatasetVariable.IdResolver resolver = DatasetVariable.IdResolver.from(path);
    if (resolver.getType() == null || DatasetVariable.Type.Collected.equals(resolver.getType())) {
      StudyDataset ds = collectedDatasetService.findById(resolver.getDatasetId());
      BaseStudyTable studyTable = ds.getStudyTable();
      BaseStudy study = studyService.findStudy(studyTable.getStudyId());
      return new RQLFieldReferences(path, ds, studyTable, study, getDatasetVariableInternal(Indexer.PUBLISHED_VARIABLE_INDEX, Indexer.VARIABLE_TYPE, path));
    } else if (DatasetVariable.Type.Dataschema.equals(resolver.getType())) {
      HarmonizationDataset ds = harmonizedDatasetService.findById(resolver.getDatasetId());
      BaseStudy study = studyService.findStudy(ds.getHarmonizationTable().getStudyId());
      return new RQLFieldReferences(path, ds, ds.getBaseStudyTables(), study, getDatasetVariableInternal(Indexer.PUBLISHED_VARIABLE_INDEX, Indexer.VARIABLE_TYPE, path));
    } else if (DatasetVariable.Type.Harmonized.equals(resolver.getType())) {
      HarmonizationDataset ds = harmonizedDatasetService.findById(resolver.getDatasetId());
      Optional<BaseStudyTable> studyTable = ds.getBaseStudyTables().stream().filter(st -> st.getStudyId().equals(resolver.getStudyId())
        && st.getSourceURN().equals(resolver.getSourceURN())).findFirst();
      if (!studyTable.isPresent()) throw new IllegalArgumentException("Not a valid variable: " + path);
      BaseStudy study = studyService.findStudy(studyTable.get().getStudyId());
      return new RQLFieldReferences(path, ds, studyTable.get(), study, getDatasetVariableInternal(Indexer.PUBLISHED_HVARIABLE_INDEX, Indexer.HARMONIZED_VARIABLE_TYPE, path));
    }
    throw new IllegalArgumentException("Not a valid variable: " + path);
  }

  private String parseValue(Object value) {
    if (value instanceof ASTNode) return toString((ASTNode) value);
    if (value instanceof Collection) {
      Collection<?> values = (Collection) value;
      if (values.size() == 1 && getValueType().isDateTime()) return parseSingleDate(values.iterator().next());
      return parseValues(values);
    }
    if (value instanceof DateTime) return parseSingleDate(value);
    return getNature().equals(VariableNature.CATEGORICAL) ? quote(value) : normalizeString(value.toString());
  }

  private String normalizeString(String str) {
    return str.replaceAll(" ", "+");
  }

  private String normalizeDate(DateTime date) {
    return DATE_FORMAT.format(date.toDate());
  }

  private String quote(Object value) {
    String valueStr = value.toString();
    return valueStr.contains("*") ? normalizeString(valueStr) : "\"" + valueStr + "\"";
  }

  private String parseRange(Object value) {
    if (value instanceof Collection) {
      return join(",", (Collection) value);
    }
    return value + ",*";
  }

  private String parseSingleDate(Object value) {
    if (value instanceof DateTime) {
      String dateString = DATE_FORMAT.format(((DateTime) value).toDate());
      return ">=" + dateString + " AND " + "<=" + dateString;
    }
    return normalizeString(value.toString());
  }

  private void parseNodes(Collection<?> args) {
    args.forEach(arg -> parseNode((ASTNode) arg));
  }

  private String parseValues(Collection<?> args) {
    return join(",", args);
  }


  private DatasetVariable getDatasetVariableInternal(String indexName, String indexType, String variableId) {
    InputStream inputStream = searcher.getDocumentById(indexName, indexType, variableId);
    if (inputStream == null) throw new NoSuchVariableException(variableId);
    try {
      return objectMapper.readValue(inputStream, DatasetVariable.class);
    } catch (IOException e) {
      throw new NoSuchVariableException(variableId);
    }
  }

}
