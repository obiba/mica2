<!-- File browser, vuejs template -->
<#macro filesBrowser>
  <div class="files-browser" v-cloak>
    <div v-if="folder.children">
      <div>
        <folder-breadcrumb v-bind:path="path" v-bind:folder="folder" v-bind:tr="tr" v-bind:context-path="contextPath"
                           v-on:select-folder="onSelectFolder"></folder-breadcrumb>
      </div>
      <files-table v-bind:folder="folder" v-bind:tr="tr" v-bind:locale="locale" v-bind:context-path="contextPath"
                   v-on:select-folder="onSelectFolder"></files-table>
    </div>
    <div v-else>
      <span class="text-muted"><@message "no-files"/></span>
    </div>
  </div>
</#macro>
