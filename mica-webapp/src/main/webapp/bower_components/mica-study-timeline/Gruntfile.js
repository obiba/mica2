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
        dest: "<%= meta.pkg.name %>.min.css"
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
        banner: "/*! <%= meta.pkg.name %> - v<%= meta.pkg.version %>\n" +
          " *  License: <%= meta.pkg.license %>\n" +
          " *  Date: <%= grunt.template.today('yyyy-mm-dd') %>\n" +
          " */\n"
      },
      dist: {
        files: [
          { "<%= meta.pkg.name %>.js": "<%= meta.srcFiles %>" },
          { "<%= meta.pkg.name %>.css": "<%= meta.cssFiles %>" }
        ]
      },
      release: {
        files: [
          { "<%= meta.pkg.name %>.js": "<%= meta.srcFiles %>" },
          { "<%= meta.pkg.name %>.css": "<%= meta.cssFiles %>" }
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
          "<%= meta.pkg.name %>.min.js": "<%= meta.pkg.name %>.js"
        }
      },
      release: {
        files: {
          "<%= meta.pkg.name %>.min.js": "<%= meta.pkg.name %>.js"
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
