var gulp = require('gulp');
var connect = require('gulp-connect');
var uglify = require('gulp-uglify');
var concat = require('gulp-concat');
var rename = require('gulp-rename');
var templateCache = require('gulp-angular-templatecache');
var streamqueue = require('streamqueue');
var fs = require('fs');

gulp.task('default', ['minify', 'connect', 'watch']);

gulp.task('connect', function () {
  connect.server({
    root: ['demo', './'],
    livereload: true,
    port: 8888
  });
});

gulp.task('reload', ['minify'], function () {
  gulp.src('./dist/**/*.*').pipe(connect.reload());
});

gulp.task('watch', function () {
  gulp.watch(['./src/**', './demo/**'], ['reload']);
});

gulp.task('minify', function () {
  var files = JSON.parse(fs.readFileSync('sources.json', 'utf-8'));
  var stream = streamqueue({ objectMode: true },
    gulp.src(['src/templates/**/*.html']).pipe(templateCache({
      standalone: true,
      root: 'src/templates/',
    })),
    gulp.src(files)
  )
  .pipe(concat('sf-obiba-file-upload.js'))
  .pipe(gulp.dest('./dist'))
  .pipe(uglify())
  .pipe(rename('sf-obiba-file-upload.min.js'))
  .pipe(gulp.dest('./dist'));

  return stream;
});
