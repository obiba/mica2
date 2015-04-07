module.exports = function (grunt) {

  "use strict";

  // name is pulled from the package.json file's 'name'

  grunt.initConfig({
    meta: {
      pkg: grunt.file.readJSON("package.json"),
      srcFiles: [
        "src/d3-timeline.js",
        "src/study-dto-parser.js",
        "src/color-generator.js",
        "src/mica-study-timeline.js"
      ],
      cssFiles: [
        "src/timeline.css"
      ]
    },
    watch: {
      scripts: {
        files: ["src/**/*.js"],
        tasks: ["jshint"]
      }
    },
    cssmin : {
      css:{
        src: "src/*.css",
        dest: "dist/<%= meta.pkg.name %>.min.css"
      }
    },
    jshint: {
      options: {
        indent: 2,
        globalstrict: true,
        laxcomma: true
      },
      chart: {
        options: {
          browser: true,
          globals: {
            d3: true
          }
        },
        files: {
          src: "<%= meta.srcFiles %>"
        }
      },
      grunt: {
        options: {
          node: true
        },
        files: {
          src: ["Gruntfile.js"]
        }
      }
    },
    concat: {
      options: {
        banner: "/*Copyright (c) 2015 OBiBa. All rights reserved.\n" +
        "* This program and the accompanying materials\n"+
        "* are made available under the terms of the GNU Public License v3.0.\n"+
        "* You should have received a copy of the GNU General Public License\n"+
        "* along with this program.  If not, see  <http://www.gnu.org/licenses>\n\n"+
        "* <%= meta.pkg.name %> - v<%= meta.pkg.version %>\n" +
        "* Date: <%= grunt.template.today('yyyy-mm-dd') %>\n" +
        " */\n"
      },
      dist: {
        files: [
          { "dist/<%= meta.pkg.name %>.js": "<%= meta.srcFiles %>" },
          { "dist/<%= meta.pkg.name %>.css": "<%= meta.cssFiles %>" }
        ]
      },
      release: {
        files: [
          { "dist/<%= meta.pkg.name %>.js": "<%= meta.srcFiles %>" },
          { "dist/<%= meta.pkg.name %>.css": "<%= meta.cssFiles %>" }
        ]
      }
    },
    uglify: {
      options: {
        // Preserve banner
        preserveComments: "some"
      },
      dist: {
        files: {
          "dist/<%= meta.pkg.name %>.min.js": "<%= meta.srcFiles %>"
        }
      },
      release: {
        files: {
          "dist/<%= meta.pkg.name %>.min.js": "<%= meta.srcFiles %>"
        }
      }
    }
  });

  grunt.loadNpmTasks("grunt-contrib-jshint");
  grunt.loadNpmTasks("grunt-contrib-concat");
  grunt.loadNpmTasks("grunt-contrib-uglify");
  grunt.loadNpmTasks("grunt-contrib-watch");
  grunt.loadNpmTasks('grunt-contrib-cssmin');

  grunt.registerTask("default", ["jshint", "concat:dist", "uglify:dist", 'cssmin:css']);
  grunt.registerTask("release", ["jshint", "concat", "uglify", 'cssmin']);
};
