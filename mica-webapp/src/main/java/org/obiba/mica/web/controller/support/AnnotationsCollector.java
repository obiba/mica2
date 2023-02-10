package org.obiba.mica.web.controller.support;

import org.obiba.mica.core.domain.Attribute;
import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.micaConfig.service.TaxonomiesService;
import org.obiba.mica.study.domain.BaseStudy;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AnnotationsCollector {
  public static Map<LocalizedString, Map<LocalizedString, List<LocalizedString>>> collectAndCount(List<BaseStudy> studies, TaxonomiesService taxonomiesService) {
    return collectInternal(studies, true, taxonomiesService);
  }
  public static Map<LocalizedString, Map<LocalizedString, List<LocalizedString>>> collect(List<BaseStudy> studies, TaxonomiesService taxonomiesService) {
    return collectInternal(studies, false, taxonomiesService);
  }

  private static Map<LocalizedString, Map<LocalizedString, List<LocalizedString>>> collectInternal(List<BaseStudy> studies,
                                                                                                   boolean addCounts,
                                                                                                   TaxonomiesService taxonomiesService) {
    // Exclude irrelevant taxonomies
    List<String> exclusions = new ArrayList<String>() {{
      add("Mlstr_harmo");
      add("Mlstr_dataschema");
    }};

    List<Taxonomy> taxonomies = taxonomiesService.getAllVariableTaxonomies().stream()
      .filter(taxonomy -> !exclusions.contains(taxonomy.getName()))
      .collect(Collectors.toList());

    // Extract inferred attributes (variable based attributes)
    Map<String, Map<String, Integer>> counts = new HashMap<>();
    List<Set<Attribute>> mergedAttributesList = studies.stream().map(BaseStudy::getMergedAttributes).collect(Collectors.toList());

    // TODO separate the study and Initiative counts
    // NOTE Might need config to have annotations for Initiative/Protocol
    if (addCounts) {
      calculateCounts(counts, mergedAttributesList.stream().filter(attributes -> !attributes.isEmpty()).collect(Collectors.toList()));
    }

    List<Attribute> mergedAttributes = mergedAttributesList.stream().flatMap(Set::stream).collect(Collectors.toList());
    Map<String, Map<String, List<LocalizedString>>> groupedAttributes = mergedAttributes.stream()
      .collect(
        Collectors.groupingBy(
          Attribute::getNamespace,
          LinkedHashMap::new, Collectors.groupingBy(
            Attribute::getName,
            Collectors.mapping(attribute -> Optional.ofNullable(attribute.getValues()).orElse(new LocalizedString()), Collectors.toList())
          )
        )
      );

    // Convert attributes to taxonomy entities to facilitate client translation and rendering as well as preserving their order
    Map<LocalizedString, Map<LocalizedString, List<LocalizedString>>> annotations = new LinkedHashMap<>();
    taxonomies.forEach(taxonomy -> {
      if (groupedAttributes.containsKey(taxonomy.getName())) {
        Map<String, List<LocalizedString>> inferredVocNames = groupedAttributes.get(taxonomy.getName());
        Map<LocalizedString, List<LocalizedString>> vocTranslations = new LinkedHashMap<>();
        taxonomy.getVocabularies().forEach(vocabulary -> {
          if (inferredVocNames.containsKey(vocabulary.getName())) {
            List<String> inferredTermValues = inferredVocNames.get(vocabulary.getName()).stream()
              .filter(localizedString -> !localizedString.isEmpty())
              .map(LocalizedString::getUndetermined)
              .collect(Collectors.toList());
            List<LocalizedString> terms = vocabulary.getTerms().stream()
              .filter(term -> inferredTermValues.contains(term.getName()))
              .map(term -> LocalizedString.from(term.getTitle()))
              .collect(Collectors.toList());

            vocTranslations.put(LocalizedString.from(vocabulary.getTitle()), terms.isEmpty() ? new ArrayList<>() : terms);
          }
        });

        if (vocTranslations.size() > 0) annotations.put(LocalizedString.from(taxonomy.getTitle()), vocTranslations);
      }
    });

    return annotations;
  }

  private static void calculateCounts(Map<String, Map<String, Integer>> counts, List<Set<Attribute>> attributesList) {
    Map<String, Integer> taxMap = new HashMap<>();
    Map<String, Integer> vocMap = new HashMap<>();
    Map<String, Integer> termMap = new HashMap<>();

    IntStream.range(0, attributesList.size()).forEach(i -> {
      Set<Attribute> inferredAttributes = attributesList.get(i);
      inferredAttributes.forEach(attribute -> {
        taxMap.put(attribute.getNamespace(), i + 1);
        vocMap.put(attribute.getNamespace()+"."+attribute.getName(), i + 1);
        termMap.put(attribute.getNamespace()+"."+attribute.getName()+"."+attribute.getValues().getUndetermined(), i + 1);
      });
    });

    counts.put("taxonomyCounts", taxMap);
    counts.put("vocabularyCounts", vocMap);
    counts.put("termCounts", termMap);
  }
}
