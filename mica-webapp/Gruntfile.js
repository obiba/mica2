/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

// Generated on 2014-03-27 using generator-jhipster 0.12.0
'use strict';

// # Globbing
// for performance reasons we're only matching one level down:
// 'test/spec/{,*/}*.js'
// use this if you want to recursively match all subfolders:
// 'test/spec/**/*.js'

var proxySnippet = require('grunt-connect-proxy2/lib/utils').proxyRequest;

module.exports = function (grunt) {
  require('load-grunt-tasks')(grunt);
  require('time-grunt')(grunt);

  grunt.initConfig({
    yeoman: {
      // configurable paths
      app: require('./bower.json').appPath || 'app',
      dist: 'src/main/webapp/dist'
    },
    less: {
      development: {
        options: {
          compress: true,
          yuicompress: true,
          optimization: 2,
          sourceMap: true
        },
        files: {
          // target.css file: source.less file
          'src/main/webapp/styles/mica.css': 'src/main/webapp/less/mica.less'
        }
      }
    },
    watch: {
      styles: {
        files: ['src/main/webapp/less/{,*/}*.less'],
        tasks: ['less'],
        options: {
          nospawn: true
        }
      },
      livereload: {
        options: {
          livereload: 35729
        },
        files: [
          'src/main/webapp/{,*/}*.html',
          '.tmp/styles/{,*/}*.css',
          '{.tmp/,}src/main/webapp/app/{,*/}*.js',
          'src/main/webapp/images/{,*/}*.{png,jpg,jpeg,gif,webp,svg}'
        ]
      }
    },
    autoprefixer: {
      dist: {
        files: [
          {
            expand: true,
            cwd: '.tmp/styles/',
            src: '{,*/}*.css',
            dest: '.tmp/styles/'
          }
        ]
      }
    },
    connect: {
      proxies: [
        {
          context: '/ws',
          host: 'localhost',
          port: 8082,
          https: false,
          changeOrigin: false
        },
        {
          context: '/jvm',
          host: 'localhost',
          port: 8082,
          https: false,
          changeOrigin: false
        },
        {
          context: '/dump',
          host: 'localhost',
          port: 8082,
          https: false,
          changeOrigin: false
        }
      ],
      options: {
        port: 9000,
        // Change this to 'localhost' to deny access to the server from outside.
        hostname: '0.0.0.0',
        livereload: 35729
      },
      livereload: {
        options: {
          open: true,
          base: [
            '.tmp',
            'src/main/webapp'
          ],
          middleware: function (connect) {
            return [
              proxySnippet,
              connect.static(require('path').resolve('src/main/webapp'))
            ];
          }
        }
      },
      test: {
        options: {
          port: 9001,
          base: [
            '.tmp',
            'test',
            'src/main/webapp'
          ]
        }
      },
      dist: {
        options: {
          base: '<%= yeoman.dist %>'
        }
      }
    },
    clean: {
      dist: {
        files: [
          {
            dot: true,
            src: [
              '.tmp',
              '<%= yeoman.dist %>/*',
              '!<%= yeoman.dist %>/.git*'
            ]
          }
        ]
      },
      server: '.tmp'
    },

    jshint: {
      options: {
        jshintrc: '.jshintrc'
      },
      all: [
        'Gruntfile.js',
        'src/main/webapp/app/**/*.js',
        'src/main/webapp/mica.js'
      ]
    },

    coffee: {
      options: {
        sourceMap: true,
        sourceRoot: ''
      },
      dist: {
        files: [
          {
            expand: true,
            cwd: 'src/main/webapp/app',
            src: '{,*/}*.coffee',
            dest: '.tmp/scripts',
            ext: '.js'
          }
        ]
      },
      test: {
        files: [
          {
            expand: true,
            cwd: 'test/spec',
            src: '{,*/}*.coffee',
            dest: '.tmp/spec',
            ext: '.js'
          }
        ]
      }
    },
    // generated dynamically by useminPrepare
    //concat: {
    //  dist: {}
    //},
    rev: {
      dist: {
        files: {
          src: [
            '<%= yeoman.dist %>/app/{,*/}*.js',
            '<%= yeoman.dist %>/styles/{,*/}*.css',
            '<%= yeoman.dist %>/images/{,*/}*.{png,jpg,jpeg,gif,webp,svg}',
            '<%= yeoman.dist %>/fonts/*'
          ]
        }
      }
    },
    useminPrepare: {
      html: 'src/main/webapp/{,*/}*.html',
      options: {
        dest: '<%= yeoman.dist %>',
        flow: {
          html: {
            steps: {
              css: ['concat', 'cssmin'],
              js: ['concat', 'uglifyjs']
            },
            post: {}
          }
        }
      }
    },
    usemin: {
      html: ['<%= yeoman.dist %>/{,*/}*.html'],
      css: ['<%= yeoman.dist %>/styles/{,*/}*.css'],
      options: {
        dirs: ['<%= yeoman.dist %>']
      }
    },
    imagemin: {
      dist: {
        files: [
          {
            expand: true,
            cwd: 'src/main/webapp/images',
            src: '{,*/}*.{jpg,jpeg}', // we don't optimize PNG files as it doesn't work on Linux. If you are not on Linux, feel free to use '{,*/}*.{png,jpg,jpeg}'
            dest: '<%= yeoman.dist %>/images'
          }
        ]
      }
    },
    svgmin: {
      dist: {
        files: [
          {
            expand: true,
            cwd: 'src/main/webapp/images',
            src: '{,*/}*.svg',
            dest: '<%= yeoman.dist %>/images'
          }
        ]
      }
    },
    // generated dynamically by useminPrepare
    //cssmin: {
    //  // By default, your `admin.html` <!-- Usemin Block --> will take care of
    //  // minification. This option is pre-configured if you do not wish to use
    //  // Usemin blocks.
    //  dist: {
    //    files: {
    //      '<%= yeoman.dist %>/styles/main.css': [
    //        '.tmp/styles/{,*/}*.css',
    //        'styles/{,*/}*.css'
    //      ]
    //    }
    //  }
    //},
    htmlmin: {
      dist: {
        options: {
          /*removeCommentsFromCDATA: true,
           // https://github.com/yeoman/grunt-usemin/issues/44
           //collapseWhitespace: true,
           collapseBooleanAttributes: true,
           removeAttributeQuotes: true,
           removeRedundantAttributes: true,
           useShortDoctype: true,
           removeEmptyAttributes: true,
           removeOptionalTags: true*/
        },
        files: [
          {
            expand: true,
            cwd: 'src/main/webapp',
            src: ['*.html', 'app/**/*.html'],
            dest: '<%= yeoman.dist %>'
          }
        ]
      }
    },
    // Put files not handled in other tasks here
    copy: {
      dist: {
        files: [
          {
            expand: true,
            dot: true,
            cwd: 'src/main/webapp',
            dest: '<%= yeoman.dist %>',
            src: [
              '*.{ico,png,txt}',
              '.htaccess',
              'images/{,*/}*.{png,gif,webp}',
              'fonts/*',
              'styles/*'
            ]
          },
          {
            expand: true,
            dot: true,
            cwd: 'src/main/webapp/bower_components/font-awesome',
            dest: '<%= yeoman.dist %>',
            src: [
              'fonts/fontawesome*'
            ]
          },
          {
            expand: true,
            dot: true,
            cwd: 'src/main/webapp/bower_components/ng-obiba-mica/dist',
            dest: '<%= yeoman.dist %>',
            src: [
              'fonts/obiba*'
            ]
          },
          {
            expand: true,
            cwd: '.tmp/images',
            dest: '<%= yeoman.dist %>/images',
            src: [
              'generated/*'
            ]
          },
          {
            expand: true,
            dot: true,
            cwd: 'src/main/webapp/bower_components/ace-builds/src-min-noconflict',
            dest: '<%= yeoman.dist %>/scripts',
            src: [
              'mode-json.js',
              'worker-json.js',
              'mode-css.js',
              'worker-css.js',
              'theme-monokai.js',
              'ext-searchbox.js'
            ]
          }
        ]
      },
      distLibsImages: {
        files: [
          {
            expand: true,
            cwd: 'src/main/webapp/bower_components/ng-obiba-mica/dist',
            dest: '<%= yeoman.dist %>',
            src: [
              'images/*'
            ]
          }
        ]
      },
      styles: {
        expand: true,
        cwd: 'src/main/webapp/styles',
        dest: '.tmp/styles/',
        src: '{,*/}*.css'
      }
    },
    concurrent: {
      server: [
        'copy:styles'
      ],
      test: [
        'copy:styles'
      ],
      dist: [
        'copy:styles',
        'imagemin',
        'svgmin',
        'htmlmin'
      ]
    },
    karma: {
      unit: {
        configFile: 'src/test/javascript/karma.conf.js',
        singleRun: true
      }
    },
    cdnify: {
      dist: {
        html: ['<%= yeoman.dist %>/*.html']
      }
    },
    wiredep: {
      task: {
        src: 'src/main/resources/templates/admin.ftl'
      }
    }
    // generated dynamically by useminPrepare
    //uglify: {
    //  dist: {
    //    files: {
    //      '<%= yeoman.dist %>/scripts/scripts.js': [
    //        '<%= yeoman.dist %>/scripts/scripts.js'
    //      ]
    //    }
    //  }
    //}
  });

  grunt.registerTask('server', function (target) {
    if (target === 'dist') {
      return grunt.task.run(['build', 'connect:dist:keepalive']);
    }

    grunt.task.run([
      'clean:server',
      'jshint',
      'concurrent:server',
      'autoprefixer',
      'configureProxies',
      'connect:livereload',
      'watch'
    ]);
  });

  grunt.registerTask('test', [
//    'clean:server',
//    'concurrent:test',
//    'autoprefixer',
//    'connect:test',
//    'karma'
  ]);

  grunt.registerTask('build', [
    'clean:dist',
    'less',
    'jshint',
    //'useminPrepare',
    'concurrent:dist',
    //'autoprefixer',
    //'concat',
    'copy:dist',
    //'cssmin',
    //'uglify',
    //'rev',
    //'usemin',
    'copy:distLibsImages'
  ]);

  grunt.registerTask('default', [
    'test',
    'build'
  ]);

  grunt.loadNpmTasks('grunt-wiredep');
};
