import type { AxiosResponse } from 'axios';
import { defineStore } from 'pinia';
import { api } from 'src/boot/api';
import type { NetworkDto } from 'src/models/Mica';

export const useNetworksStore = defineStore('networks', () => {
  const networks = ref<NetworkDto[]>([]);

  async function fetchNetworks(from: number = 0, limit: number = 1000, order: string = 'asc', sort: string = 'id') {
    return api.get('/draft/networks', { params: { from, limit, order, sort } }).then((response: AxiosResponse) => {
      if (response.status === 200) {
        networks.value = response.data;
      }
      return response;
    });
  }

  return {
    networks,
    fetchNetworks,
  };
});