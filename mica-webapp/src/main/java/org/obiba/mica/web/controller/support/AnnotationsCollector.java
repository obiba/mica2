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

  private static final String TAXONOMY_COUNTS = "taxonomyCounts";
  private static final String VOCABULARY_COUNTS = "vocabularyCounts";
  private static final String TERM_COUNTS = "termCounts";

  public static Map<String, TaxonomyAnnotationItem> collectAndCount(List<BaseStudy> studies, TaxonomiesService taxonomiesService) {
    return collectInternal(studies, true, taxonomiesService);
  }
  public static Map<String, TaxonomyAnnotationItem> collect(List<BaseStudy> studies, TaxonomiesService taxonomiesService) {
    return collectInternal(studies, false, taxonomiesService);
  }

  private static Map<String, TaxonomyAnnotationItem> collectInternal(List<BaseStudy> studies,
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
    Map<String, Map<String, Integer>> counts = new HashMap<String, Map<String, Integer>>() {{
      put(TAXONOMY_COUNTS, new HashMap<>());
      put(VOCABULARY_COUNTS, new HashMap<>());
      put(TERM_COUNTS, new HashMap<>());
    }};

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
    Map<String, TaxonomyAnnotationItem> annotations = new LinkedHashMap<>();
    taxonomies.forEach(taxonomy -> {
      String taxonomyName = taxonomy.getName();

      if (groupedAttributes.containsKey(taxonomyName)) {
        Map<String, List<LocalizedString>> attVocabularyNames = groupedAttributes.get(taxonomyName);
        Map<String, VocabularyAnnotationItem> vocTranslations = new LinkedHashMap<>();
        taxonomy.getVocabularies().forEach(vocabulary -> {
          String vocabularyName = vocabulary.getName();

          if (attVocabularyNames.containsKey(vocabularyName)) {
            List<String> attTermValues = attVocabularyNames.get(vocabularyName).stream()
              .filter(localizedString -> !localizedString.isEmpty())
              .map(LocalizedString::getUndetermined)
              .collect(Collectors.toList());

            List<TermItem> terms = vocabulary.getTerms().stream()
              .filter(term -> attTermValues.contains(term.getName()))
              .map(term ->
                new TermItem(
                  term.getName(),
                  LocalizedString.from(term.getTitle()),
                  addCounts ? counts.get(TERM_COUNTS).get(taxonomyName+"."+vocabularyName+"."+term.getName()) : -1)
              )
              .collect(Collectors.toList());

            vocTranslations.put(
              vocabularyName,
              new VocabularyAnnotationItem(
                LocalizedString.from(vocabulary.getTitle()),
                terms.isEmpty() ? new ArrayList<>() : terms,
                addCounts ? counts.get(VOCABULARY_COUNTS).get(taxonomyName+"."+vocabularyName) : -1
              )
            );
          } else {
            vocTranslations.put(
              vocabularyName,
              new VocabularyAnnotationItem(
                LocalizedString.from(vocabulary.getTitle()),
                new ArrayList<>(),
                -1
              ).notPresent()
            );
          }
        });

        if (vocTranslations.size() > 0) {
          annotations.put(
            taxonomyName,
            new TaxonomyAnnotationItem(
              LocalizedString.from(taxonomy.getTitle()),
              vocTranslations,
              addCounts ? counts.get(TAXONOMY_COUNTS).get(taxonomyName) : -1)
          );
        }
      }
    });

    return annotations;
  }

  private static void calculateCounts(Map<String, Map<String, Integer>> counts, List<Set<Attribute>> attributesList) {
    Map<String, Integer> taxMap = counts.get(TAXONOMY_COUNTS);
    Map<String, Integer> vocMap = counts.get(VOCABULARY_COUNTS);
    Map<String, Integer> termMap = counts.get(TERM_COUNTS);

    IntStream.range(0, attributesList.size()).forEach(i -> {
      Set<Attribute> inferredAttributes = attributesList.get(i);
      inferredAttributes.forEach(attribute -> {
        taxMap.put(attribute.getNamespace(), i + 1);
        vocMap.put(attribute.getNamespace()+"."+attribute.getName(), i + 1);
        termMap.put(attribute.getNamespace()+"."+attribute.getName()+"."+attribute.getValues().getUndetermined(), i + 1);
      });
    });
  }

  private static abstract class AnnotationItem {
    protected final LocalizedString title;
    protected final int count;

    AnnotationItem(LocalizedString title, int count) {
      this.title = title;
      this.count = count;
    }

    public LocalizedString getTitle() {
      return title;
    }
    public int getCount() {
      return count;
    }
  }

  public static class TaxonomyAnnotationItem extends AnnotationItem {
    private final Map<String, VocabularyAnnotationItem> vocabularies;
     public TaxonomyAnnotationItem(LocalizedString title, Map<String, VocabularyAnnotationItem> vocabularies, int count) {
       super(title, count);
       this.vocabularies = vocabularies;
     }
    public Map<String, VocabularyAnnotationItem> getVocabularies() {
      return vocabularies;
    }
  }

  public static class VocabularyAnnotationItem extends AnnotationItem{
    private final List<TermItem> terms;
    private boolean missing = false;

     public VocabularyAnnotationItem(LocalizedString title, List<TermItem> terms, int count) {
       super(title, count);
       this.terms = terms;
     }

     public VocabularyAnnotationItem notPresent() {
       missing = true;
       return this;
     }

    public List<TermItem> getTerms() {
      return terms;
    }

    public boolean isMissing() {
      return missing;
    }
  }
  public static class TermItem extends AnnotationItem{
    private final LocalizedString term;

    public TermItem(String name, LocalizedString title, int count) {
      super(title, count);
      this.term = title;
    }

    public LocalizedString getTerm() {
      return term;
    }
  }
}
