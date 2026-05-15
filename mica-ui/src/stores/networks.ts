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
    fetchNetworkCommits,
  };
});
