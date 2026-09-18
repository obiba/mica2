<template>
  <acl-panel :endpoints="fileAclEndpoints(path)" :can-edit="canEdit" :stacked="stacked" />
</template>

<script setup lang="ts">
import AclPanel from 'src/components/permissions/AclPanel.vue';
import { fileAclEndpoints } from 'src/composables/useAcl';

interface Props {
  /** the path of the file or folder */
  path: string;
  /** the user can change the lists (publish permission on the file) */
  canEdit?: boolean;
  /** the lists one below the other (narrow column) */
  stacked?: boolean;
}

defineProps<Props>();
</script>
