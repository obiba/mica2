//a fun little thing to minify the rql javascript
//the mangle option is turned off due to all the tricky "call function by name from another function that exists in string format" stuff that happens in RQL

module.exports = function(grunt) {

    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),
        uglify: {
            options: {
                mangle: false
            },
            my_target: {
                files: {
                    'js/rql.min.js': ['js/rql.js']
                }
            }
        }
    });
    
    grunt.loadNpmTasks('grunt-contrib-uglify');

};