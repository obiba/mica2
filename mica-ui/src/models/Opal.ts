// hand-written subset of the Opal DTOs served by Mica (the Makefile only generates the Mica protos)

export interface LocaleTextDto {
  locale: string;
  text: string;
}

export interface VocabularySummaryDto {
  name: string;
  title: LocaleTextDto[];
}

/** `GET /taxonomies/_summary` */
export interface TaxonomySummaryDto {
  name: string;
  title: LocaleTextDto[];
  vocabularySummaries?: VocabularySummaryDto[] | undefined;
}

export interface TaxonomiesDto {
  summaries: TaxonomySummaryDto[];
}
