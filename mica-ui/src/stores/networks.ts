import type { AxiosResponse } from 'axios';
import { defineStore } from 'pinia';
import { api } from 'src/boot/api';
import type { GitCommitInfoDto, NetworkDto } from 'src/models/Mica';

export const useNetworksStore = defineStore('networks', () => {
  const networks = ref<NetworkDto[]>([]);
  const network = ref<NetworkDto | null>(null);

  async function fetchNetworks(from: number = 0, limit: number = 1000, order: string = 'asc', sort: string = 'id') {
    return api.get('/draft/networks', { params: { from, limit, order, sort } }).then((response: AxiosResponse) => {
      if (response.status === 200) {
        networks.value = response.data;
      }
      return response;
    });
  }

  async function fetchNetwork(id: string) {
    return api.get<NetworkDto>(`/draft/network/${id}`).then((response: AxiosResponse) => {
      if (response.status === 200) {
        network.value = response.data;
      }
      return response;
    });
  }

  /** a network to be created */
  function newNetwork(): NetworkDto {
    return {
      name: [],
      acronym: [],
      description: [],
      investigators: [],
      contacts: [],
      attachments: [],
      studyIds: [],
      studySummaries: [],
      memberships: [],
      networkIds: [],
      networkSummaries: [],
      membershipSortOrder: [],
      published: false,
    };
  }

  /** creates the network, resolves to its id */
  async function createNetwork(dto: NetworkDto): Promise<string> {
    const response = await api.post('/draft/networks', dto);
    const location: string = response.headers['location'] ?? '';
    return location.substring(location.lastIndexOf('/') + 1);
  }

  async function saveNetwork(dto: NetworkDto, comment?: string): Promise<void> {
    await api.put(`/draft/network/${dto.id}`, dto, { params: comment ? { comment } : {} });
  }

  async function fetchNetworkCommits(id: string): Promise<GitCommitInfoDto[]> {
    return api.get<GitCommitInfoDto[]>(`/draft/network/${id}/commits`).then((response: AxiosResponse) => {
      if (response.status === 200) {
        return response.data;
      }
      return response;
    });
  }

  return {
    networks,
    network,
    fetchNetworks,
    fetchNetwork,
    newNetwork,
    createNetwork,
    saveNetwork,
    fetchNetworkCommits,
  };
});
