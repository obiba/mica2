package org.obiba.mica.web.controller.domain;

import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.obiba.mica.access.domain.StatusChange;

import java.util.*;
import java.util.stream.Collectors;

public class DataAccessEntityDiff {

  private final StatusChange statusChange;

  private final Map<String, Map<String, List<Object>>> diff;

  private final DiffRowGenerator generator;

  public DataAccessEntityDiff(StatusChange statusChange, Map<String, Map<String, List<Object>>> diff) {
    this.statusChange = statusChange;
    this.diff = diff;
    //create a configured DiffRowGenerator
    this.generator = DiffRowGenerator.create()
      .showInlineDiffs(true)
      .mergeOriginalRevised(false)
      .inlineDiffByWord(true)
      .ignoreWhiteSpaces(false)
      .oldTag(f -> f ? "<del>" : "</del>")     // or use markdown style for strikethrough: ~~
      .newTag(f -> f ? "<ins>" : "</ins>")     // or use markdown style for bold: **
      .build();
  }

  public StatusChange getStatusChange() {
    return statusChange;
  }

  public boolean hasDifferences() {
    return !getDifferences().isEmpty();
  }

  /**
   * Merge the different differences types and filter/sort keys.
   *
   * @return differences
   */
  public Map<String, List<Object>> getDifferences() {
    Map<String, List<Object>> diffs = Maps.newTreeMap();
    getDiffering().entrySet().stream()
      .filter(entry -> includeDiffKey(entry.getKey()))
      .forEach(entry -> {
        List<Object> newValue = Lists.newArrayList(entry.getValue());
        List<DiffRow> rows = generator.generateDiffRows(
          Collections.singletonList(entry.getValue().get(1) == null ? "" : entry.getValue().get(1).toString()),
          Collections.singletonList(entry.getValue().get(2) == null ? "" : entry.getValue().get(2).toString()));
        newValue.set(1, rows.stream()
          .map(DiffRow::getOldLine)
          .reduce((a, b) -> a + "<br/>" + b)
          .orElse(""));
        newValue.set(2, rows.stream()
          .map(DiffRow::getNewLine)
          .reduce((a, b) -> a + "<br/>" + b)
          .orElse(""));
        diffs.put(cleanDiffKey(entry.getKey()), newValue);
      });
    getOnlyLeft().entrySet().stream()
      .filter(entry -> includeDiffKey(entry.getKey()))
      .forEach(entry -> {
        List<Object> newValue = entry.getValue().stream()
          .map(val -> val == null ? null : val.toString())
          .collect(Collectors.toList());
        newValue.add("");
        diffs.put(cleanDiffKey(entry.getKey()), newValue);
      });
    getOnlyRight().entrySet().stream()
      .filter(entry -> includeDiffKey(entry.getKey()))
      .forEach(entry -> {
        List<Object> newValue = entry.getValue().stream()
          .map(val -> val == null ? null : val.toString())
          .collect(Collectors.toList());
        newValue.add(1, "");
        diffs.put(cleanDiffKey(entry.getKey()), newValue);
      });

    return diffs;
  }

  //
  // Private methods
  //

  private String cleanDiffKey(String key) {
    return key.replaceAll(".obibaFiles", "");
  }

  private boolean includeDiffKey(String key) {
    return !key.contains(".obibaFiles[") || key.endsWith(".fileName") || key.endsWith(".size");
  }

  private Map<String, List<Object>> getDiffering() {
    return diff.get("differing");
  }

  private Map<String, List<Object>> getOnlyLeft() {
    return diff.get("onlyLeft");
  }

  private Map<String, List<Object>> getOnlyRight() {
    return diff.get("onlyRight");
  }

}
