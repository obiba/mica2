'use strict';

module.exports = function (grunt) {

  grunt.initConfig({

    meta: {
      pkg: grunt.file.readJSON('package.json'),
      src: {
        js: [
          // order is important!
          'src/ng-obiba-mica.js',
          'src/utils/utils.js',
          'src/file/file.js',
          'src/file/file-filter.js',
          'src/file/file-service.js',
          'src/attachment/attachment.js',
          'src/attachment/attachment-directives.js',
          'src/access/data-access-request.js',
          'src/access/data-access-request-controller.js',
          'src/access/data-access-request-router.js',
          'src/access/data-access-request-service.js',
          'src/search/search.js',
          'src/search/search-filter.js',
          'src/search/search-rql.js',
          'src/search/search-service.js',
          'src/search/search-controller.js',
          'src/search/search-directive.js',
          'src/search/search-router.js',
          'src/graphics/graphics.js',
          'src/graphics/graphics-directive.js',
          'src/graphics/graphics-controller.js',
          'src/graphics/graphics-service.js',
          'src/localized/localized.js',
          'src/localized/localized-directives.js',
          'src/localized/localized-service.js',
          'src/localized/localized-filter.js',
          'src/file-browser/file-browser.js',
          'src/file-browser/file-browser-directive.js',
          'src/file-browser/file-browser-controller.js',
          'src/file-browser/file-browser-service.js'
        ]
      }
    },

    less: {
      development: {
        options: {
          sourceMap: true,
          sourceMapBasepath: '"dist/css/',
          compress: true,
          yuicompress: true,
          optimization: 2
        },
        files: {
          "dist/css/ng-obiba-mica.css": "less/ng-obiba-mica.less" // destination file and source file
        }
      }
    },

    clean: {
      build: ['<%= destination_dir %>/bower_components', 'tmp', 'dist'],
      tmp: ['tmp']
    },

    karma: {
      unit: {
        configFile: 'test/karma.conf.js',
        singleRun: true
      }
    },

    /* convert AngularJs html templates to cached JavaScript */
    html2js: {
      ngObibaMica: {
        options: {},
        src: ['src/**/*.html'],
        dest: 'tmp/<%= meta.pkg.name %>.templates.js'
      }
    },

    concat: {
      options: {
        separator: ';',
        banner: '/*!\n' +
        ' * <%= meta.pkg.name %> - v<%= meta.pkg.version %>\n' +
        ' * <%= meta.pkg.homepage %>\n\n' +
        ' * License: <%= meta.pkg.license %>\n' +
        ' * Date: <%= grunt.template.today("yyyy-mm-dd") %>\n' +
        ' */\n'
      },
      dist: {
        src: ['<%= meta.src.js %>', 'tmp/*.js'],
        dest: 'dist/<%= meta.pkg.name %>.js'
      }
    },

    uglify: {
      options: {
        // Preserve banner
        preserveComments: 'some'
      },
      dist: {
        files: {
          'dist/<%= meta.pkg.name %>.min.js': ['<%= concat.dist.dest %>']
        }
      }
    },

    jshint: {
      files: ['src/**/*.js'],
      options: {
        jshintrc: '.jshintrc'
      }
    },

    copy: {
      dist: {
        files: [{
          expand: true,
          dot: true,
          cwd: 'images/',
          dest: 'dist/images/',
          src: ['**']
        }, {
          expand: true,
          dot: true,
          cwd: 'fonts/',
          dest: 'dist/fonts/',
          src: ['**']
        }]
      }
    },
    watch: {
        files: [
          'src/**/*.html',
          'src/**/*.js',
          'less/**/*.less'
        ],
        tasks: ['default']
    }
  });

  grunt.loadNpmTasks('grunt-contrib-copy');
  grunt.loadNpmTasks('grunt-contrib-less');
  grunt.loadNpmTasks('grunt-contrib-clean');
  grunt.loadNpmTasks('grunt-contrib-concat');
  grunt.loadNpmTasks('grunt-contrib-jshint');
  grunt.loadNpmTasks('grunt-contrib-uglify');
  grunt.loadNpmTasks('grunt-html2js');
  grunt.loadNpmTasks('grunt-karma');
  grunt.loadNpmTasks('grunt-contrib-watch');

  // TODO uncomment below and remove last line once unit tests are implemented
  // grunt.registerTask('default', ['clean:build', 'less', 'jshint', 'html2js', 'concat', 'clean:tmp', 'karma', 'uglify', 'copy']);
  grunt.registerTask('default', ['clean:build', 'less', 'jshint', 'html2js', 'concat', 'clean:tmp', 'uglify', 'copy']);
  grunt.registerTask('watchChanges', ['default', 'watch']);
};
