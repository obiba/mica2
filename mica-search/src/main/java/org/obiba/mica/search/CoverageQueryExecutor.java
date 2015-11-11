package org.obiba.mica.search;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.AttributeKey;
import org.obiba.mica.micaConfig.service.OpalService;
import org.obiba.mica.web.model.Dtos;
import org.obiba.mica.web.model.MicaSearch;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.obiba.opal.core.domain.taxonomy.Term;
import org.obiba.opal.core.domain.taxonomy.Vocabulary;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Component
@Scope("request")
public class CoverageQueryExecutor {

  @Inject
  private OpalService opalService;

  @Inject
  private JoinQueryExecutor joinQueryExecutor;

  @Inject
  private Dtos dtos;

  private MicaSearch.JoinQueryDto joinQueryDto;

  public MicaSearch.TaxonomiesCoverageDto coverageQuery(List<String> taxonomyNames,
    MicaSearch.JoinQueryDto joinQueryDto) throws IOException {

    this.joinQueryDto = joinQueryDto == null ? getDefaultJoinQueryDto() : joinQueryDto;

    // We need the aggregations internally for building the coverage result,
    // but we may not need them in the final result
    MicaSearch.JoinQueryDto joinQueryDtoWithFacets = MicaSearch.JoinQueryDto.newBuilder().mergeFrom(this.joinQueryDto)
      .setWithFacets(true).build();

    MicaSearch.JoinQueryResultDto result = joinQueryExecutor
      .queryCoverage(JoinQueryExecutor.QueryType.VARIABLE, joinQueryDtoWithFacets);

    List<MicaSearch.AggregationResultDto> aggregations = ungroupAggregations(
      result.getVariableResultDto().getAggsList());

    MicaSearch.TaxonomiesCoverageDto.Builder builder = MicaSearch.TaxonomiesCoverageDto.newBuilder()//
      .setTotalCount(result.getVariableResultDto().getTotalCount()) //
      .setTotalHits(result.getVariableResultDto().getTotalHits()) //
      .addAllTaxonomies(getCoverages(taxonomyNames, aggregations));

    // Do not append the aggregations if no facets is requested
    if(this.joinQueryDto.getWithFacets()) builder.setQueryResult(result);

    return builder.build();
  }

  private MicaSearch.JoinQueryDto getDefaultJoinQueryDto() {
    MicaSearch.JoinQueryDto.Builder builder = MicaSearch.JoinQueryDto.newBuilder() //
      .setWithFacets(false);
    return builder.build();
  }

  /**
   * Extract hits from aggregations and merge them into the taxonomies descriptions.
   *
   * @param taxonomyNames
   * @param aggregations
   * @return
   */
  private Iterable<MicaSearch.TaxonomyCoverageDto> getCoverages(Collection<String> taxonomyNames,
    List<MicaSearch.AggregationResultDto> aggregations) {
    Map<String, Map<String, MicaSearch.TermsAggregationResultDto>> aggTermsTitlesMap = aggregations.stream().collect(
      Collectors.toMap(MicaSearch.AggregationResultDto::getAggregation,
        a -> a.getExtension(MicaSearch.TermsAggregationResultDto.terms).stream()
          .collect(Collectors.toMap(MicaSearch.TermsAggregationResultDto::getKey, t -> t))));

    Map<String, List<BucketResult>> bucketResultsByTaxonomy = extractBucketResults(aggregations).stream()
      .collect(Collectors.groupingBy(BucketResult::getTaxonomy));

    Map<String, Map<String, Integer>> aggsMap = Maps.newHashMap();
    aggregations.forEach(agg -> {
      String name = agg.getAggregation();
      List<MicaSearch.TermsAggregationResultDto> results = agg.getExtension(MicaSearch.TermsAggregationResultDto.terms);
      if(results != null && !results.isEmpty() && name.startsWith("attributes-") && name.endsWith("-und")) {
        String key = name.replaceAll("^attributes-", "").replaceAll("-und$", "");
        if(!aggsMap.containsKey(key)) aggsMap.put(key, Maps.newHashMap());
        results.forEach(res -> aggsMap.get(key).put(res.getKey(), res.getCount()));
      }
    });

    List<MicaSearch.TaxonomyCoverageDto> coverages = Lists.newArrayList();
    getTaxonomies().stream().filter(
      taxonomy -> taxonomyNames == null || taxonomyNames.isEmpty() || taxonomyNames.contains(taxonomy.getName()))
      .forEach(
        taxonomy -> addTaxonomyCoverage(coverages, taxonomy, aggsMap, bucketResultsByTaxonomy.get(taxonomy.getName()),
          aggTermsTitlesMap));

    return coverages;
  }

  private List<MicaSearch.AggregationResultDto> ungroupAggregations(List<MicaSearch.AggregationResultDto> aggsList) {
    List<MicaSearch.AggregationResultDto> newList = Lists.newArrayList();
    aggsList.stream().forEach(agg -> {
      if(agg.getChildrenCount() > 0) {
        newList.addAll(agg.getChildrenList());
      } else {
        newList.add(agg);
      }
    });

    return newList;
  }

  /**
   * Extract the hits per taxonomy term and bucket.
   *
   * @param aggregations
   * @return
   */
  @NotNull
  private Collection<BucketResult> extractBucketResults(List<MicaSearch.AggregationResultDto> aggregations) {
    if(joinQueryDto == null || !joinQueryDto.hasVariableQueryDto() ||
      joinQueryDto.getVariableQueryDto().getAggsByCount() == 0) return Collections.emptyList();

    List<String> aggsBy = joinQueryDto.getVariableQueryDto().getAggsByList();

    List<BucketResult> termResults = Lists.newArrayList();

    aggregations.stream().filter(agg -> aggsBy.contains(agg.getAggregation())).forEach(bucket -> {
      String bucketField = bucket.getAggregation(); // studyIds for instance
      bucket.getExtension(MicaSearch.TermsAggregationResultDto.terms).stream() //
        .filter(agg -> agg.getAggsCount() > 0) //
        .forEach(agg -> agg.getAggsList().stream() //
          .filter(t -> !t.getExtension(MicaSearch.TermsAggregationResultDto.terms).isEmpty()) //
          .forEach(t -> termResults.addAll(BucketResult.list(bucketField, agg.getKey(), t))));
    });

    Collections.sort(termResults);

    return termResults;
  }

  /**
   * For a {@link org.obiba.opal.core.domain.taxonomy.Taxonomy}, report the number of hits and optionally the
   * number of hits for each bucket.
   *
   * @param coverages
   * @param taxonomy
   * @param aggsMap
   * @param bucketResults
   */
  private void addTaxonomyCoverage(List<MicaSearch.TaxonomyCoverageDto> coverages, Taxonomy taxonomy,
    Map<String, Map<String, Integer>> aggsMap, @Nullable List<BucketResult> bucketResults,
    Map<String, Map<String, MicaSearch.TermsAggregationResultDto>> aggTermsTitlesMap) {
    if(taxonomy.hasVocabularies()) {
      MicaSearch.TaxonomyCoverageDto.Builder taxoBuilder = MicaSearch.TaxonomyCoverageDto.newBuilder();
      taxoBuilder.setTaxonomy(dtos.asDto(taxonomy, joinQueryDto.getLocale()));
      List<Integer> hits = Lists.newArrayList();
      String namespace = taxonomy.getName().equals("Default") ? null : taxonomy.getName();
      Map<String, List<BucketResult>> bucketResultsByVocabulary = bucketResults == null
        ? Maps.newHashMap()
        : bucketResults.stream().collect(Collectors.groupingBy(BucketResult::getVocabulary));

      taxonomy.getVocabularies().forEach(vocabulary -> hits.add(addVocabularyCoverage(taxoBuilder, vocabulary,
        aggsMap.get(AttributeKey.getMapKey(vocabulary.getName(), namespace)),
        bucketResults == null ? null : bucketResultsByVocabulary.get(vocabulary.getName()), aggTermsTitlesMap)));

      taxoBuilder.setHits(hits.isEmpty() ? 0 : hits.stream().mapToInt(x -> x).sum());
      // compute the sum of the hits for all vocabularies per bucket
      if(bucketResults != null) {
        Map<String, List<BucketResult>> bucketResultsByBucketField = bucketResults.stream()
          .collect(Collectors.groupingBy(BucketResult::getBucketField));

        bucketResultsByBucketField.keySet().forEach(field -> {
          Map<String, List<BucketResult>> bucketResultsByBucketValue = bucketResultsByBucketField.get(field).stream()
            .collect(Collectors.groupingBy(BucketResult::getBucketValue));

          bucketResultsByBucketValue.keySet().stream().sorted().forEach(value -> {
            List<BucketResult> buckets = bucketResultsByBucketValue.get(value);
            int sumOfHits = buckets.stream().mapToInt(BucketResult::getHits).sum();
            if(sumOfHits > 0) {
              taxoBuilder.addBuckets(
                getBucketCoverageDtoBuilder(field, value, sumOfHits, aggTermsTitlesMap.get(field).get(value)));
            }
          });
        });
      }

      if(!taxoBuilder.getVocabulariesList().isEmpty()) {
        coverages.add(taxoBuilder.build());
      }
    }
  }

  /**
   * For a taxonomy {@link org.obiba.opal.core.domain.taxonomy.Vocabulary}, report the number of hits and optionally the
   * number of hits for each bucket.
   *
   * @param taxoBuilder
   * @param vocabulary
   * @param hits
   * @param bucketResults
   * @return
   */
  private int addVocabularyCoverage(MicaSearch.TaxonomyCoverageDto.Builder taxoBuilder, Vocabulary vocabulary,
    Map<String, Integer> hits, @Nullable List<BucketResult> bucketResults,
    Map<String, Map<String, MicaSearch.TermsAggregationResultDto>> aggTermsTitlesMap) {
    int sumOfHits = 0;
    if(vocabulary.hasTerms()) {
      Map<String, List<BucketResult>> bucketResultsByTerm = bucketResults == null
        ? Maps.newHashMap()
        : bucketResults.stream().collect(Collectors.groupingBy(BucketResult::getTerm));

      MicaSearch.VocabularyCoverageDto.Builder vocBuilder = MicaSearch.VocabularyCoverageDto.newBuilder();
      vocBuilder.setVocabulary(dtos.asDto(vocabulary, joinQueryDto.getLocale()));
      vocabulary.getTerms().forEach(
        term -> addTermCoverage(vocBuilder, term, hits, bucketResultsByTerm.get(term.getName()), aggTermsTitlesMap));
      // only one term can be applied at a time, then the sum of the term hits is the number of variables
      // that cover this vocabulary
      sumOfHits = hits == null ? 0 : hits.values().stream().mapToInt(x -> x).sum();
      vocBuilder.setHits(sumOfHits);

      if(!vocabulary.isRepeatable()) {
        vocBuilder.setCount(sumOfHits);
      }

      // compute the sum of the hits for all terms per bucket
      if(bucketResults != null) {
        Map<String, List<BucketResult>> bucketResultsByBucketField = bucketResults.stream()
          .collect(Collectors.groupingBy(BucketResult::getBucketField));

        bucketResultsByBucketField.keySet().forEach(field -> {
          Map<String, List<BucketResult>> bucketResultsByBucketValue = bucketResultsByBucketField.get(field).stream()
            .collect(Collectors.groupingBy(BucketResult::getBucketValue));

          bucketResultsByBucketValue.keySet().stream().sorted().forEach(value -> {
            List<BucketResult> buckets = bucketResultsByBucketValue.get(value);
            int sumOfBucketHits = buckets.stream().mapToInt(BucketResult::getHits).sum();
            if(sumOfBucketHits > 0) {
              MicaSearch.BucketCoverageDto.Builder builder = getBucketCoverageDtoBuilder(field, value, sumOfBucketHits,
                aggTermsTitlesMap.get(field).get(value));

              if(!vocabulary.isRepeatable()) builder.setCount(builder.getHits());

              vocBuilder.addBuckets(builder);
            }
          });
        });
      }
      if(!vocBuilder.getTermsList().isEmpty()) {
        taxoBuilder.addVocabularies(vocBuilder);
      } else {
        sumOfHits = 0;
      }
    }
    return sumOfHits;
  }

  /**
   * For a taxonomy {@link org.obiba.opal.core.domain.taxonomy.Term}, report the number of hits and optionally
   * the number of hits for each bucket.
   *
   * @param vocBuilder
   * @param term
   * @param hits
   * @param bucketResults
   */
  private void addTermCoverage(MicaSearch.VocabularyCoverageDto.Builder vocBuilder, Term term,
    Map<String, Integer> hits, @Nullable List<BucketResult> bucketResults,
    Map<String, Map<String, MicaSearch.TermsAggregationResultDto>> aggTermsTitlesMap) {
    MicaSearch.TermCoverageDto.Builder termBuilder = MicaSearch.TermCoverageDto.newBuilder();
    termBuilder.setTerm(dtos.asDto(term, joinQueryDto.getLocale()));
    termBuilder.setHits(0);
    // add the hits per buckets
    if(bucketResults != null) {
      termBuilder.addAllBuckets(bucketResults.stream().filter(b -> b.getHits() > 0).map(
        b -> getBucketCoverageDtoBuilder(b.getBucketField(), b.getBucketValue(), b.getHits(),
          aggTermsTitlesMap.get(b.getBucketField()).get(b.getBucketValue())).build()).collect(Collectors.toList()));
    }

    if(hits != null && hits.containsKey(term.getName())) termBuilder.setHits(hits.get(term.getName()));

    if(termBuilder.getHits() > 0) {
      vocBuilder.addTerms(termBuilder);
    }
  }

  @NotNull
  private MicaSearch.BucketCoverageDto.Builder getBucketCoverageDtoBuilder(String field, String value, int hits,
    @NotNull MicaSearch.TermsAggregationResultDto term) {
    MicaSearch.BucketCoverageDto.Builder builder = MicaSearch.BucketCoverageDto.newBuilder().setField(field)
      .setValue(value).setHits(hits);

    if(term.hasTitle()) builder.setTitle(term.getTitle());
    if(term.hasDescription()) builder.setDescription(term.getDescription());

    return builder;
  }

  @NotNull
  private List<Taxonomy> getTaxonomies() {
    List<Taxonomy> taxonomies = null;
    try {
      taxonomies = opalService.getTaxonomies();
    } catch(Exception e) {
      // ignore
    }
    return taxonomies == null ? Collections.emptyList() : taxonomies;
  }

  /**
   * The number of variable hits per bucket and taxonomy term.
   */
  private static class BucketResult implements Comparable<BucketResult> {

    private final String bucketField;

    private final String bucketValue;

    private final String taxonomy;

    private final String vocabulary;

    private final String term;

    private final int hits;

    private BucketResult(@Nullable String bucketField, @Nullable String bucketValue, String taxonomy, String vocabulary,
      String term, int hits) {
      this.bucketField = bucketField;
      this.bucketValue = bucketValue;
      this.taxonomy = taxonomy;
      this.vocabulary = vocabulary;
      this.term = term;
      this.hits = hits;
    }

    private static Collection<BucketResult> list(@Nullable String bucketField, @Nullable String bucketValue,
      MicaSearch.AggregationResultDto agg) {
      String key = agg.getAggregation().replaceAll("^attributes-", "").replaceAll("-und$", "");
      AttributeKey attrKey = AttributeKey.from(key);
      String taxonomy = attrKey.hasNamespace(null) ? "Default" : attrKey.getNamespace();
      String vocabulary = attrKey.getName();
      return agg.getExtension(MicaSearch.TermsAggregationResultDto.terms).stream()
        .map(t -> new BucketResult(bucketField, bucketValue, taxonomy, vocabulary, t.getKey(), t.getCount()))
        .collect(Collectors.toList());
    }

    public String getBucketField() {
      return bucketField;
    }

    public String getBucketValue() {
      return bucketValue;
    }

    public String getTaxonomy() {
      return taxonomy;
    }

    public String getVocabulary() {
      return vocabulary;
    }

    public String getTerm() {
      return term;
    }

    public int getHits() {
      return hits;
    }

    public String toString() {
      return "[" + bucketField + "," + bucketValue + "," + taxonomy + "," + vocabulary + "," + term + "]=" + hits;
    }

    @Override
    public int hashCode() {
      return getBucketField().hashCode() + getBucketValue().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof BucketResult && obj.toString().equals(toString());
    }

    @Override
    public int compareTo(BucketResult o) {
      return getBucketValue().compareTo(o.getBucketValue());
    }
  }

}
