package org.obiba.mica.core.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

public class RegexHashMapTest {

  @Test
  public void normalMapTest() {
    RegexHashMap map = new RegexHashMap();

    map.put("populations[2].model.selectionCriteria.criteria", "criteria");
    map.put("populations[0].dataCollectionEvents[2].model.startDate", "startDate");
    map.put("model.objectives", "objectives");

    Assert.assertEquals("criteria", map.get("populations2.model.selectionCriteria,criteria"));
    Assert.assertEquals(null, map.get("populations[0].dataCollectionEvents[2].model.startDate"));
    Assert.assertEquals("objectives", map.get("model.objectives"));
  }

  @Test
  public void regexMapTest() {
    RegexHashMap map = new RegexHashMap();

    map.put("^populations\\[\\d+\\]\\.model\\.selectionCriteria\\.criteria$", "criteria");
    map.put("^populations\\[\\d+\\]\\.dataCollectionEvents\\[\\d+\\]\\.model\\.startDate$", "startDate");
    map.put("^model\\.objectives$", "objectives");

    Assert.assertEquals("criteria", map.get("populations[2].model.selectionCriteria.criteria"));
    Assert.assertEquals("criteria", map.get("populations[12].model.selectionCriteria.criteria"));

    Assert.assertEquals("startDate", map.get("populations[0].dataCollectionEvents[2].model.startDate"));
    Assert.assertEquals("startDate", map.get("populations[14].dataCollectionEvents[3].model.startDate"));
    Assert.assertEquals(null, map.get("populations[4].dataCollectionEvents[-2].model.startDate"));

    Assert.assertEquals("objectives", map.get("model.objectives"));
  }

  @Test
  public void regexQuoteTest() {
    RegexHashMap map = new RegexHashMap();

    map.put("^populations\\[\\d+\\]\\.model\\.selectionCriteria\\." + Pattern.quote("criteria"), "criteria");
    map.put("^populations\\[\\d+\\]\\.dataCollectionEvents\\[\\d+\\]\\." + Pattern.quote("model.startDate"), "startDate");
    map.put(Pattern.quote("model.objectives"), "objectives");

    Assert.assertEquals("criteria", map.get("populations[2].model.selectionCriteria.criteria"));
    Assert.assertEquals("startDate", map.get("populations[0].dataCollectionEvents[2].model.startDate"));
    Assert.assertEquals("objectives", map.get("model.objectives"));
  }

  @Test
  public void regexLocalesTest() {
    RegexHashMap map = new RegexHashMap();

    List<String> locales = Arrays.asList("fr", "en");
    String joinedLocales = locales.stream().map(locale -> "\\." + locale).collect(Collectors.joining("|"));

    List<String> singleLocale = Arrays.asList("fr");
    String joinedSingleLocale = singleLocale.stream().map(locale -> "\\." + locale).collect(Collectors.joining("|"));

    map.put("^populations\\[\\d+\\]\\.model\\.selectionCriteria\\." + Pattern.quote("criteria") + "(" + joinedLocales + ")?", "criteria");
    map.put(Pattern.quote("model.objectives") + "(" + joinedSingleLocale + ")?", "objectives");

    Assert.assertEquals("criteria", map.get("populations[2].model.selectionCriteria.criteria"));
    Assert.assertEquals("criteria", map.get("populations[2].model.selectionCriteria.criteria.en"));
    Assert.assertEquals(null, map.get("populations[2].model.selectionCriteria.criteria."));
    Assert.assertEquals("objectives", map.get("model.objectives"));
    Assert.assertEquals("objectives", map.get("model.objectives.fr"));
    Assert.assertEquals(null, map.get("model.objectives.en"));
  }
}