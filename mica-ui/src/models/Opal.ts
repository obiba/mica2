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

export interface AttributeDto {
  key: string;
  value: string;
}

export interface TermDto {
  name: string;
  title?: LocaleTextDto[];
  description?: LocaleTextDto[];
  keywords?: LocaleTextDto[];
  attributes?: AttributeDto[];
  terms?: TermDto[];
}

export interface VocabularyDto extends TermDto {
  repeatable?: boolean;
}

/** `GET /taxonomies/_filter`, `GET /meta-taxonomy` */
export interface TaxonomyDto {
  name: string;
  author?: string;
  license?: string;
  title?: LocaleTextDto[];
  description?: LocaleTextDto[];
  keywords?: LocaleTextDto[];
  attributes?: AttributeDto[];
  vocabularies?: VocabularyDto[];
}

/** an Opal project, as listed by `/config/opal-projects` and `/draft/study-state/{id}/opal-projects` */
export interface ProjectDto {
  name: string;
  title?: string;
  datasource?: { name: string; table?: string[] };
}
