package org.obiba.mica.dataset.search.rest.variable;

import org.obiba.mica.web.model.Mica;
import org.obiba.mica.web.model.MicaSearch;
import org.obiba.mica.web.model.MicaSearch.ChartsCoverageDto;

public class CoverageChartDataFactory {

  public ChartsCoverageDto makeChartData(MicaSearch.TaxonomiesCoverageDto taxonomiesCoverageDto) {
    ChartsCoverageDto.Builder chartsBuilder = ChartsCoverageDto.newBuilder();
    taxonomiesCoverageDto.getTaxonomiesList()
      .stream()
      .filter(dto -> dto.hasHits() && dto.getHits() > 0)
      .forEach(taxonomyCoverageDto -> {
        Mica.TaxonomyEntityDto taxonomy = taxonomyCoverageDto.getTaxonomy();
        ChartsCoverageDto.ChartDto.Builder chartBuilder = ChartsCoverageDto.ChartDto.newBuilder();

        taxonomyCoverageDto.getVocabulariesList()
          .forEach(vocabularyCoverageDto -> {
            Mica.TaxonomyEntityDto vocabulary = vocabularyCoverageDto.getVocabulary();
            String title = vocabulary.getTitlesCount() > 0 ? vocabulary.getTitles(0).getValue() : "";
            ChartsCoverageDto.ChartDataDto.Builder chartDataBuilder = ChartsCoverageDto.ChartDataDto.newBuilder();
            chartDataBuilder.setVocabulary(vocabulary.getName());

            if (vocabularyCoverageDto.getBucketsCount() > 0) {
              vocabularyCoverageDto.getBucketsList().forEach(bucket ->
                chartDataBuilder
                  .addItems(MicaSearch.ChartsCoverageDto.ChartDataItemDto.newBuilder()
                    .setValue(bucket.getHits())
                    .setKey(bucket.getValue())
                    .setTitle(title)
                  ));
            } else {
              chartDataBuilder
                .addItems(MicaSearch.ChartsCoverageDto.ChartDataItemDto.newBuilder()
                  .setValue(vocabularyCoverageDto.getCount())
                  .setKey("")
                  .setTitle(title)
                );
            }

            chartBuilder.addData(chartDataBuilder);
          });

        String title = taxonomy.getTitlesCount() > 0 ? taxonomy.getTitles(0).getValue() : "";
        String subtitle = taxonomy.getDescriptionsCount() > 0 ? taxonomy.getDescriptions(0).getValue() : "";
        chartsBuilder.addCharts(chartBuilder.setTitle(title).setSubtitle(subtitle).setTaxonomy(taxonomy.getName()));
      });

    return chartsBuilder.build();
  }
}
