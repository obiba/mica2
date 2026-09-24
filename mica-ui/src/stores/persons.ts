import { defineStore } from 'pinia';
import { api, toServerUrl } from 'src/boot/api';
import type { PersonDto, PersonsDto } from 'src/models/Mica';
import { checkDuplicates, duplicateQuery, type DuplicateCheck } from 'src/utils/persons';

export interface PersonsSearch {
  /** a search query, see `searchQuery()` */
  query?: string | undefined;
  from: number;
  limit: number;
  sort: string;
  order: 'asc' | 'desc';
  /** a person id left out, just deleted but maybe still indexed */
  exclude?: string | undefined;
}

export const usePersonsStore = defineStore('persons', () => {
  async function search(params: PersonsSearch): Promise<PersonsDto> {
    const response = await api.get<PersonsDto>('/draft/persons/_search', { params });
    return { ...response.data, persons: response.data.persons ?? [] };
  }

  /** the CSV of the persons matching the query */
  function downloadUrl(query: string | undefined, total: number): string {
    const params = new URLSearchParams({ limit: String(Math.max(total, 1)) });
    if (query) params.set('query', query);
    return toServerUrl(`/draft/persons/_search/_download?${params}`);
  }

  async function get(id: string): Promise<PersonDto> {
    return (await api.get<PersonDto>(`/draft/person/${id}`)).data;
  }

  async function create(person: PersonDto): Promise<PersonDto> {
    return (await api.post<PersonDto>('/draft/persons', person)).data;
  }

  async function update(person: PersonDto): Promise<PersonDto> {
    return (await api.put<PersonDto>(`/draft/person/${person.id}`, person)).data;
  }

  async function remove(id: string): Promise<void> {
    await api.delete(`/draft/person/${id}`);
  }

  /** the other persons with the same name, and the same email among them */
  async function findDuplicates(person: PersonDto): Promise<DuplicateCheck> {
    const query = duplicateQuery(person);
    if (query === '') return {};
    const found = await search({ query, from: 0, limit: 100, sort: 'lastName', order: 'asc' });
    return checkDuplicates(person, found);
  }

  return { search, downloadUrl, get, create, update, remove, findDuplicates };
});
