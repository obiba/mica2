<!-- File browser, vuejs template -->
<#macro filesBrowser>
  <div class="files-browser" v-cloak>
    <div v-if="folder.children">
      <div>
        <folder-breadcrumb v-bind:path="path" v-bind:folder="folder" v-bind:tr="tr"
                           v-on:select-folder="onSelectFolder"></folder-breadcrumb>
      </div>
      <div>
        <table class="table table-sm table-stripped">
          <thead>
          <tr>
            <th>#</th>
            <th><@message "name"/></th>
            <th><@message "description"/></th>
            <th><@message "size"/></th>
            <th><@message "actions"/></th>
          </tr>
          </thead>
          <tbody>
          <tr is="file-row" v-for="file in folder.children" v-bind:key="file.name"
              v-bind:file="file" v-bind:tr="tr" v-bind:locale="locale"
              v-on:select-folder="onSelectFolder"></tr>
          </tbody>
        </table>
      </div>
    </div>
    <div v-else>
      <span class="text-muted"><@message "no-files"/></span>
    </div>
  </div>
</#macro>
