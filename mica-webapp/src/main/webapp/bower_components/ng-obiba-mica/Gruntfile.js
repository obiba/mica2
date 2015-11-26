'use strict';

module.exports = function (grunt) {

  grunt.initConfig({

    meta: {
      pkg: grunt.file.readJSON('package.json'),
      src: {
        js: [
          // order is important!
          'src/access/data-access-request.js',
          'src/access/data-access-request-controller.js',
          'src/access/data-access-request-router.js',
          'src/access/data-access-request-service.js',
          'src/ng-obiba-mica.js'
        ]
      }
    },

    //less: {
    //  development: {
    //    options: {
    //      compress: true,
    //      yuicompress: true,
    //      optimization: 2
    //    },
    //    files: {
    //      "dist/css/ng-obiba-mica.css": "less/ng-obiba-mica.less" // destination file and source file
    //    }
    //  }
    //},

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
    }
  });

  //grunt.loadNpmTasks('grunt-contrib-less');
  grunt.loadNpmTasks('grunt-contrib-clean');
  grunt.loadNpmTasks('grunt-contrib-concat');
  grunt.loadNpmTasks('grunt-contrib-jshint');
  grunt.loadNpmTasks('grunt-contrib-uglify');
  grunt.loadNpmTasks('grunt-html2js');

  //grunt.registerTask('default', ['clean:build', 'less', 'jshint', 'html2js', 'concat', 'clean:tmp', 'uglify']);
  grunt.registerTask('default', ['clean:build', 'jshint', 'html2js', 'concat', 'clean:tmp', 'uglify']);

};