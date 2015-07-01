module.exports = function(grunt) {
    // Project configuration.
    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),
        sass: {                              // Task
            dist: {                            // Target
                trace: true,
                options: {                       // Target options
                    style: 'expanded'
                },
                files: {
                    'public/stylesheets/main.css': 'public/stylesheets/main.scss'
                }
            }
        },
        watch: {
            scripts: {
                files: ['public/**/*.scss'],
                tasks: ['sass'],
                options: {
                    spawn: false
                }
            }
        }
    });

    // Load the plugin that provides the SASS compile task.
    grunt.loadNpmTasks('grunt-contrib-sass')
    grunt.loadNpmTasks('grunt-contrib-watch');

    // Default task(s).
    grunt.registerTask('default', ['sass']);

};
