# D3 Timeline for Mica Studies

## Building

1. Run `bower install` to install `d3` and `d3.chart` into `bower_components`
1. Run `npm install -d` to install the grunt modules
1. Run `grunt` to build the concatenated and minified versions in the project root. To run [jshint](http://www.jshint.com) continually on file changes in the `src` directory, run `grunt watch`.

## Usage

```javascript
new $.MicaTimeline(new $.StudyDtoParser()).create("#vis", getTestData()).addLegend();
```
