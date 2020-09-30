'use strict';

Vue.component('folder-breadcrumb', {
  props: {
    path: String,   // relative path
    folder: Object, // current folder
    tr: Object      // translations
  },
  computed: {
    segments: function() {
      if (this.isRoot) {
        return [];
      } else {
        let tokens = this.path.split('/').slice(1);
        const segments = [];
        let spath = this.folder.path.slice(0, this.folder.path.lastIndexOf(this.path));
        tokens.forEach((token, idx) => {
          if (idx === tokens.length - 1) {
            segments.push({
              token: token,
              class: 'breadcrumb-item active'
            });
          } else {
            spath = spath + '/' + token;
            segments.push({
              path: spath,
              token: token,
              class: 'breadcrumb-item'
            });
          }
        });
        return segments;
      }
    },
    isRoot: function() {
      return this.path === '/' || this.path === '';
    }
  },
  template:
    '<div>' +
    '<ol class="breadcrumb breadcrumb-sm float-left">' +
    '<li v-if="isRoot" class="breadcrumb-item active"><i class="fas fa-home"></i></li>' +
    '<li v-else class="breadcrumb-item"><a href="javascript:void(0)" v-on:click="$emit(\'select-folder\', \'/\')"><i class="fas fa-home"></i></a></li>' +
    '<li v-for="segment in segments" v-bind:class="segment.class">' +
    '  <a v-if="segment.path" href="javascript:void(0)" v-on:click="$emit(\'select-folder\', segment.path)">{{ segment.token }}</a>' +
    '  <span v-else>{{ segment.token }}</span>' +
    '</li>' +
    '</ol>' +
    '<a :href="\'/ws/file-dl\' + folder.path" class="btn btn-sm btn-info float-right"><i class="fa fa-download"></i> {{ tr.download }}</a>' +
    '</div>'
});

Vue.component('file-row', {
  data: function() {
    return {
      textMaxLength: 100
    };
  },
  props: {
    file: Object,  // the file in the row
    tr: Object,    // translations
    locale: String
  },
  computed: {
    iconClass: function() {
      return this.isFile ? 'far fa-file' : 'fa fa-folder';
    },
    isFile: function() {
      return this.file.type === 'FILE';
    },
    sizeLabel: function() {
      if (this.isFile) {
        if (this.file.size<1000) {
          return this.file.size + ' B';
        } else if (this.file.size<1000000) {
          return (this.file.size / 1000).toFixed(1) + ' KB';
        } else if (this.file.size<1000000000) {
          return (this.file.size / 1000000).toFixed(1) + ' MB';
        } else {
          return (this.file.size / 1000000000).toFixed(1) + ' GB';
        }
      } else  {
        return this.file.size + ' ' + (this.file.size === 1 ? this.tr.item : this.tr.items);
      }
    },
    hasMediaType: function() {
      return this.file.mediaType && this.file.mediaType.trim() !== '';
    },
    descriptionLabel: function() {
      if (this.isFile && this.file.description && this.file.description.length>0) {
        let desc = this.file.description.filter(item => item.lang === this.locale).pop();
        if (!desc) {
          desc = this.file.description.pop();
        }
        return desc.value.length>this.textMaxLength ? desc.value.substring(0, this.textMaxLength) + ' ...' : desc.value;
      }
      return '';
    },
    hasDescriptionTitle: function() {
      return this.descriptionTitle.length>this.textMaxLength;
    },
    descriptionTitle: function() {
      if (this.isFile && this.file.description && this.file.description.length>0) {
        let desc = this.file.description.filter(item => item.lang === this.locale).pop();
        if (!desc) {
          desc = this.file.description.pop();
        }
        return desc.value;
      }
      return '';
    }
  },
  template: '<tr>' +
    '<td><i v-bind:class="iconClass"></i></td>' +
    '<td v-if="isFile">{{ file.name }} <span class="badge badge-info" v-if="hasMediaType">{{ file.mediaType }}</span></td>' +
    '<td v-else>' +
    '  <a v-if="file.size>0" href="javascript:void(0)" v-on:click="$emit(\'select-folder\', file.path)">{{ file.name }}</a>' +
    '  <span v-else>{{ file.name }}</span></td>' +
    '<td><small>{{ descriptionLabel }}</small> <i class="fas fa-info-circle" :title="descriptionTitle" v-if="hasDescriptionTitle"></i></td>' +
    '<td>{{ sizeLabel }}</td>' +
    '<td><a v-if="file.size>0" download :href="\'/ws/file-dl\' + file.path"><i class="fa fa-download"></i></a></td>' +
    '</tr>'
});

const makeFilesVue = function(el, data, childrenFilter) {
  const vm = new Vue({
    el: el,
    data: data,
    computed: {
      rawFolder: {
        get: function() { return this.folder; },
        set: function(value) {
          const folder = value;
          if (childrenFilter && value.children) {
            folder.children = value.children.filter(f => childrenFilter(f));
            if (folder.children.length === 0) {
              delete folder.children;
            }
          }
          this.folder = folder;
        }
      }
    },
    methods: {
      onSelectFolder: function(folderPath) {
        const relativePath = folderPath === '/' ?
          this.basePath :
          (folderPath.replace('/' + this.type + '/' + this.id, ''));
        console.log(relativePath);
        const that = this;
        FilesService.getFolder(this.type, this.id, relativePath, function(data) {
          that.rawFolder = data;
          that.path = that.basePath ? relativePath.replace(that.basePath, '') : relativePath;
        }, function(response) {

        });
      }
    }
  });
  $(el + '-container').hide();
  FilesService.getFolder(vm.type, vm.id, vm.basePath + vm.path, function(data) {
    vm.rawFolder = data;
    if (vm.folder.children) {
      $(el + '-container').show();
    }
  }, function(response) {
    console.log(response);
  });
};
