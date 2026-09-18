<template>
  <acl-panel :endpoints="documentAclEndpoints(target)" :can-edit="canEdit" with-file />
</template>

<script setup lang="ts">
import AclPanel from 'src/components/permissions/AclPanel.vue';
import type { DocumentTarget } from 'src/composables/useDocumentTarget';
import { documentAclEndpoints } from 'src/composables/useAcl';

interface Props {
  target: DocumentTarget;
  /** the user can change the lists (publish permission) */
  canEdit?: boolean;
}

defineProps<Props>();
</script>
