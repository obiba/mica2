/*Copyright (c) 2015 OBiBa. All rights reserved.
* This program and the accompanying materials
* are made available under the terms of the GNU Public License v3.0.
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see  <http://www.gnu.org/licenses>

* mica-study-timeline - v1.0.0-SNAPSHOT
* Date: 2015-05-14
 */
(function () {

  "use strict";

  d3.timeline = function () {
    var DISPLAY_TYPES = ["circle", "rect"];

    var hover = function () {},
      click = function () {},
      scroll = function () {},
      orient = "bottom",
      width = null,
      height = null,
      tickFormat = {
        format: d3.format("d"),
        tickTime: 1,
        tickNumber: 1,
        tickSize: 10
      },
      colorCycle = d3.scale.category20(),
      display = "rect",
      startYear = 0,
      beginning = 0,
      ending = 0,
      margin = {left: 30, right: 30, top: 30, bottom: 30},
      stacked = false,
      rotateTicks = false,
      itemHeight = 10,
      itemMargin = 5;

    function timeline(gParent) {
      var g = gParent.append("g");
      var gParentSize = gParent[0][0].getBoundingClientRect();
      var gParentItem = d3.select(gParent[0][0]);

      var yAxisMapping = {},
        maxStack = 1,
        minTime = 0,
        maxTime = 0;

      setWidth();

      // check how many stacks we're gonna need
      // do this here so that we can draw the axis before the graph
      if (stacked || (ending === 0 && beginning === 0)) {
        g.each(function (d, i) {
          d.forEach(function (datum, index) {

            // create y mapping for stacked graph
            if (stacked && Object.keys(yAxisMapping).indexOf(index) == -1) {
              yAxisMapping[index] = maxStack;
              maxStack++;
            }

            // figure out beginning and ending times if they are unspecified
            if (ending === 0 && beginning === 0) {
              datum.events.forEach(function (event, i) {
                if (event.starting_time < minTime || minTime === 0) {
                  minTime = event.starting_time;
                }
                if (event.ending_time > maxTime) {
                  maxTime = event.ending_time;
                }
              });
            }
          });
        });

        if (ending === 0 && beginning === 0) {
          beginning = minTime;
          ending = maxTime;
        }
      }

      var scaleFactor = (1 / (ending - beginning)) * (width - margin.left - margin.right);

      var formatTime = tickFormat.format;
      var formatByYear = function (d) {
        return startYear + (parseInt(formatTime(d), null) / 12); // print in years
      };

      // draw the axis
      var xScale = d3.time.scale()
        .domain([beginning, ending])
        .range([margin.left, width - margin.right]);

      var xAxis = d3.svg.axis()
        .scale(xScale)
        .orient(orient)
        .tickFormat(formatByYear)
        .tickSubdivide(1)
        .tickValues(d3.range(beginning, ending + 1, 12))
        .tickSize(tickFormat.tickSize, tickFormat.tickSize / 2, 0);

      // draw axis
      g.append("g")
        .attr("class", "axis")
        .attr("transform", "translate(" + 0 + "," + (margin.top + (itemHeight + itemMargin) * maxStack) + ")")
        .call(xAxis);


      // draw the chart
      g.each(function (d, i) {
        d.forEach(function (datum, index) {
          var data = datum.population.events;
          var hasLabel = (typeof(datum.label) != "undefined");
          g.selectAll("svg").data(data).enter()
            .append("path")
            .attr('id', 'line-path')
            .attr("d", function drawRect(d, i) {
              var rectX = getXPos(d, i);
              var rectY = getStackPosition(d, i);
              var rectWidth = getWidth(d, i);
              return rightRoundedRect(rectX, rectY, rectWidth, itemHeight, 5);
            })
            .style("fill", datum.population.color)
            .on("mouseover", function (d, i) {
              hover(d, index, datum);
            })
            .on("click", function (d, i) {
              click(d, index, datum);
            })
            .append("title").text(function (d) {
              return d.title;
            });

          // add the label
          if (hasLabel) {
            gParent.append('text')
              .attr("class", "timeline-label")
              .attr("transform", "translate(" + 0 + "," + (itemHeight / 2 + margin.top + (itemHeight + itemMargin) * yAxisMapping[index]) + ")")
              .text(hasLabel ? datum.label : datum.id);
          }

          if (typeof(datum.icon) != "undefined") {
            gParent.append('image')
              .attr("class", "timeline-label")
              .attr("transform", "translate(" + 0 + "," + (margin.top + (itemHeight + itemMargin) * yAxisMapping[index]) + ")")
              .attr("xlink:href", datum.icon)
              .attr("width", margin.left)
              .attr("height", itemHeight);
          }

          function getStackPosition(d, i) {
            return stacked ? margin.top + (itemHeight + itemMargin) * yAxisMapping[index] : margin.top;
          }
        });
      });

      if (rotateTicks) {
        g.selectAll("text")
          .attr("transform", function (d) {
            return "rotate(" + rotateTicks + ")translate(" +
              (this.getBBox().width / 2 + 10) + "," + // TODO: change this 10
              this.getBBox().height / 2 + ")";
          });
      }

      var gSize = g[0][0].getBoundingClientRect();
      setHeight();

      function getXPos(d, i) {
        return margin.left + (d.starting_time - beginning) * scaleFactor;
      }

      function getWidth(d, i) {
        return (d.ending_time - d.starting_time) * scaleFactor;
      }

      function setHeight() {
        if (!height && !gParentItem.attr("height")) {
          if (itemHeight) {
            // set height based off of item height
            height = gSize.height + gSize.top - gParentSize.top;
            // set bounding rectangle height
            d3.select(gParent[0][0]).attr("height", height);
          } else {
            throw "height of the timeline is not set";
          }
        } else {
          if (!height) {
            height = gParentItem.attr("height");
          } else {
            gParentItem.attr("height", height);
          }
        }
      }

      function setWidth() {
        if (!width && !gParentSize.width) {
          throw "width of the timeline is not set";
        } else if (!(width && gParentSize.width)) {
          if (!width) {
            width = gParentItem.attr("width");
          } else {
            gParentItem.attr("width", width);
          }
        }
        // if both are set, do nothing
      }

      function rightRoundedRect(x, y, width, height, radius) {
        return "M" + x + "," + y +
          "h" + (width - radius) +
          "a" + radius + "," + radius + " 0 0 1 " + radius + "," + radius +
          "v" + (height - 2 * radius) +
          "a" + radius + "," + radius + " 0 0 1 " + -radius + "," + radius +
          "h" + (radius - width) +
          "z";
      }

    }

    timeline.margin = function (p) {
      if (!arguments.length) return margin;
      margin = p;
      return timeline;
    };

    timeline.orient = function (orientation) {
      if (!arguments.length) return orient;
      orient = orientation;
      return timeline;
    };

    timeline.itemHeight = function (h) {
      if (!arguments.length) return itemHeight;
      itemHeight = h;
      return timeline;
    };

    timeline.itemMargin = function (h) {
      if (!arguments.length) return itemMargin;
      itemMargin = h;
      return timeline;
    };

    timeline.height = function (h) {
      if (!arguments.length) return height;
      height = h;
      return timeline;
    };

    timeline.width = function (w) {
      if (!arguments.length) return width;
      width = w;
      return timeline;
    };

    timeline.display = function (displayType) {
      if (!arguments.length || (DISPLAY_TYPES.indexOf(displayType) == -1)) return display;
      display = displayType;
      return timeline;
    };

    timeline.tickFormat = function (format) {
      if (!arguments.length) return tickFormat;
      tickFormat = format;
      return timeline;
    };

    timeline.hover = function (hoverFunc) {
      if (!arguments.length) return hover;
      hover = hoverFunc;
      return timeline;
    };

    timeline.click = function (clickFunc) {
      if (!arguments.length) return click;
      click = clickFunc;
      return timeline;
    };

    timeline.colors = function (colorFormat) {
      if (!arguments.length) return colorCycle;
      colorCycle = colorFormat;
      return timeline;
    };

    timeline.startYear = function (b) {
      if (!arguments.length) return startYear;
      startYear = b;
      return timeline;
    };

    timeline.beginning = function (b) {
      if (!arguments.length) return beginning;
      beginning = b;
      return timeline;
    };

    timeline.ending = function (e) {
      if (!arguments.length) return ending;
      ending = e;
      return timeline;
    };

    timeline.rotateTicks = function (degrees) {
      rotateTicks = degrees;
      return timeline;
    };

    timeline.stack = function () {
      stacked = !stacked;
      return timeline;
    };

    return timeline;
  };
})();

(function ($) {

  "use strict";

  /**
   * Constructor
   * @constructor
   */
  $.StudyDtoParser = function () {
  };

  /**
   * Class method definition
   * @type {{create: create}}
   */
  $.StudyDtoParser.prototype = {

    parse: function (studyDto) {
      if (studyDto.populations) {
        return parseStudy(studyDto, findBounds(studyDto.populations));
      }

      return null;
    }
  };


      /**
   * Returns the date bounds of all population, startYear and maxYear (in months)
   * @param populations
   * @returns {{min: number, max: number, start: Number}}
   */
  function findBounds(populations) {
    var startYear = Number.MAX_VALUE;
    var maxYear = Number.MIN_VALUE;
    $.each(populations, function (i, population) {
      if (population.hasOwnProperty('dataCollectionEvents')) {
        $.each(population.dataCollectionEvents, function (j, dce) {
          startYear = Math.min(startYear, dce.startYear);
          maxYear = Math.max(maxYear, convertToMonths(dce.hasOwnProperty('endYear') ? dce.endYear - startYear : 0, dce.hasOwnProperty('endMonth') ? dce.endMonth : 12));
        });
      }
    });

    return {min: 0, max: Math.ceil(maxYear / 12) * 12, start: startYear};
  }

  /**
   * Converts to months
   * @param year
   * @param month
   * @returns {number}
   */
  function convertToMonths(year, month) {
    return 12 * parseInt(year, 10) + parseInt(month, 10);
  }

  /**
   * Converts a StudyDto to timeline compatible data format
   * @param studyDto
   * @param bounds
   */
  function parseStudy(studyDto, bounds) {
    var populations = parsePopulations(studyDto, bounds);
    if (populations.length === 0) return null;
    var timelineData = {start: bounds.start, min: 0, max: bounds.max, data: populations};
    return timelineData;
  }

  /**
   * Given a Study Dto, extracts the required fields and formats the data required for timeline rendering
   * @param studyDto
   * @param bounds
   */
  function parsePopulations(studyDto, bounds) {
    if (studyDto === null || !studyDto.hasOwnProperty('populations')) return;

    var colors = new $.ColorGenerator();
    var populations = [];
    var populationData;
    $.each(studyDto.populations, parsePopulationsInternal(populations, colors));

    return populations;


    /**
     * Defined merely to pass extra arguments to the $.each iterator closure
     * @param populations
     * @param colors
     * @returns {Function}
     */
    function parsePopulationsInternal(populations, colors) {
      return function (i, populationDto) {
        var lines = [];
        populationData = {};
        setId(populationData, populationDto, 'id');
        setTitle(populationData, populationDto, 'name');
        populationData.color = colors.nextColor();
        if (populationDto.hasOwnProperty('dataCollectionEvents') && populationDto.dataCollectionEvents.length > 0) {
          parseEvents(lines, populationData, populationDto.dataCollectionEvents, bounds);
          // use a loop instead of array.concat() in order to add lines to the same populations variable (same instance)
          $.each(lines, function(i,  line) {
            populations.push(line);
          });
        }
      };
    }

    /**
     * Sets the 'id' field if present
     * @param obj
     * @param dto
     * @param field
     */
    function setId(obj, dto, field) {
      if (dto.hasOwnProperty(field)) obj[field] = dto[field];
    }

    /**
     * Sets the title field if present and only for the first local
     * @param obj
     * @param dto
     * @param field
     */
    function setTitle(obj, dto, field) {
      if (dto.hasOwnProperty(field)) obj.title = dto[field][0].value;
    }

    /**
     * Set the data collection events of a given population
     * @param lines
     * @param populationData
     * @param dto
     * @param bounds
     */
    function parseEvents(lines, populationData, dto, bounds) {
      if (jQuery.isEmptyObject(dto)) return;

      var parsedFirstDce = false;
      if (dto[0].endYear) {
        parsedFirstDce = true;
        lines.push(createPopulationItem(populationData, dto[0], bounds));
      }

      $.each(dto, function (i, dceDto) {
        if (!dceDto.endYear || (parsedFirstDce && i === 0)) return true; // first line is already populated
        var addLine = true;
        $.each(lines, function (j, line) {
          var last = line.population.events[line.population.events.length - 1];
          var startingTime = getStartingTime(dceDto, bounds);
          var endingTime = getEndingTime(dceDto, bounds);
          if (!overlap(startingTime, endingTime, last.starting_time, last.ending_time)) {
            line.population.events.push(createEventItem(dceDto, bounds));
            addLine = false;
            return false;
          }
        });
        if (addLine && dceDto.endYear) {
          lines.push(createPopulationItem(populationData, dceDto, bounds));
        }
      });

      /**
       * Determines if two dates overlap
       * @param s1
       * @param e1
       * @param s2
       * @param e2
       * @returns {boolean}
       */
      function overlap(s1, e1, s2, e2) {
        return Math.min(e1, e2) - Math.max(s2, s1) > 0;
      }

      /**
       * Create a population item
       * @param populationData
       * @param dceDto
       * @param bounds
       * @returns {{population: *}}
       */
      function createPopulationItem(populationData, dceDto, bounds) {
        var cloneObject = jQuery.extend({}, populationData);
        cloneObject.events = [createEventItem(dceDto, bounds)];
        return {population: cloneObject};
      }

      /**
       * Creates an Data Collection Event item
       * @param dceDto
       * @param bounds
       * @returns {{}}
       */
      function createEventItem(dceDto, bounds) {
        var dce = {};
        setId(dce, dceDto, 'id');
        setTitle(dce, dceDto, 'name');
        setStartingTime(dce, dceDto, bounds);
        setEndingTime(dce, dceDto, bounds);
        return dce;
      }

      /**
       * Given a DCE returns the starting time in months
       * @param dceDto
       * @param bounds
       * @returns {number}
       */
      function getStartingTime(dceDto, bounds) {
        var start = dceDto.hasOwnProperty('startYear') ? dceDto.startYear : 0;
        var end = dceDto.hasOwnProperty('startMonth') ? dceDto.startMonth - 1 : 0;
        return convertToMonths(start - bounds.start, start > 0 ? end : 0);
      }

      /**
       * Sets the starting time of an event in months
       * @param dce
       * @param dceDto
       * @param bounds
       */
      function setStartingTime(dce, dceDto, bounds) {
        dce.starting_time = getStartingTime(dceDto, bounds);
      }

      /**
       * Given a DCE returns the ending time in months
       * @param dceDto
       * @param bounds
       * @returns {number}
       */
      function getEndingTime(dceDto, bounds) {
        var start = dceDto.hasOwnProperty('endYear') ? dceDto.endYear : 0;
        var end = dceDto.hasOwnProperty('endMonth') ? dceDto.endMonth : 12;
        return convertToMonths(start > 0 ? start - bounds.start : 1, start > 0 ? end : 0);
      }

      /**
       * Sets the ending time of an event in months
       * @param dce
       * @param dceDto
       * @param bounds
       */
      function setEndingTime(dce, dceDto, bounds) {
        dce.ending_time = getEndingTime(dceDto, bounds);
      }
    }
  }

}(jQuery));

(function ($) {

  "use strict";

  var defaultPalette = [
    '#9a3034', // Dark red
    '#886e6a', // Brownish-violet
    '#dc9351', // Tan
    '#0078ad', // Dark blue
    '#8db173', // Light green
    '#bca68e', // Light grey
    '#707175', // Dark grey
    '#ff7901', // Orange
    '#88697b', // Violet
    '#99c1c3', // Light blue
    '#306d05', // Green variation
    '#f25314'  // Orange variation
  ];

  $.ColorGenerator = function (palette) {
    this.colorPalette = jQuery.isEmptyObject(palette) ? defaultPalette : palette;
    this.index = 0;
    this.rotation = 1;
  };

  $.ColorGenerator.prototype = {
    nextColor: function () {
      // TODO need a better alternation algorithm
      var color = shadeColor(this.colorPalette[this.index], ((this.rotation % 10) === 0 ? -1 : 1) * (this.rotation) / 70);
      this.index = (this.index + 1) % this.colorPalette.length;
      if (this.index === 0) this.rotation++;
      return color;
    }
  };

  function shadeColor(color, percent) {
    // validate hex string
    var hex = String(color).replace(/[^0-9a-f]/gi, '');
    if (hex.length < 6) {
      hex = hex[0] + hex[0] + hex[1] + hex[1] + hex[2] + hex[2];
    }
    percent = percent || 0;

    // convert to decimal and change luminosity
    var rgb = "#", c, i;
    for (i = 0; i < 3; i++) {
      c = parseInt(hex.substr(i * 2, 2), 16);
      c = Math.round(Math.min(Math.max(0, c + (c * percent)), 255)).toString(16);
      rgb += ("00" + c).substr(c.length);
    }

    return rgb;
  }
}(jQuery));
(function ($) {

  "use strict";

  /**
   * Constructor
   * @constructor
   */
  $.MicaTimeline = function (dtoParser, popupIdFormatter, useBootstrapTooltip) {
    this.parser = dtoParser;
    this.popupIdFormatter = popupIdFormatter;
    this.useBootstrapTooltip = useBootstrapTooltip;
  };

  /**
   * Class method definition
   * @type {{create: create}}
   */
  $.MicaTimeline.prototype = {

    create: function (selectee, studyDto) {
      if (this.parser === null || studyDto === null) return;
      var timelineData = this.parser.parse(studyDto);
      if (timelineData) createTimeline(this, timelineData, selectee, studyDto);
      return this;
    },

    addLegend: function () {
      if (!this.timelineData || !this.timelineData.hasOwnProperty('data') || this.timelineData.data.length === 0) return;
      var ul = $("<ul id='legend' class='timeline-legend'>");

      $(this.selectee).after(ul);

      var processedPopulations = {};
      $.each(this.timelineData.data, function(i, item) {
        if (!processedPopulations.hasOwnProperty(item.population.title)) {
          processedPopulations[item.population.title] = true;
          var li = $(createLegendRow(item.population.color, item.population.title));
          ul.append(li);
        }
      });
      
      return this;
    }
  };

  function createTimeline(timeline, timelineData, selectee, studyDto) {
    var width = $(selectee).width();
    var chart = d3.timeline()
      .startYear(timelineData.start)
      .beginning(timelineData.min)
      .ending(timelineData.max)
      .width(width)
      .stack()
      .tickFormat({
        format: d3.format("d"),
        tickTime: 1,
        tickNumber: 1,
        tickSize: 10
      })
      .margin({left: 15, right: 15, top: 0, bottom: 20})
      .rotateTicks(timelineData.max > $.MicaTimeline.defaultOptions.maxMonths ? 45 : 0)
      .click(function (d, i, datum) {
        if (timeline.popupIdFormatter) {
          var popup = $(timeline.popupIdFormatter(studyDto, datum.population, d));
          if (popup.length > 0) popup.modal();
        }
      });

    d3.select(selectee).append("svg").attr("width", width).datum(timelineData.data).call(chart);

    if (timeline.useBootstrapTooltip === true) {
      d3.select(selectee).selectAll('#line-path')
        .attr('data-placement', 'top')
        .attr('data-toggle', 'tooltip')
        .attr('data-original-title', function(d){
          return d.title;
        })
        .selectAll('title').remove(); // remove default tooltip
    }

    timeline.timelineData = timelineData;
    timeline.selectee = selectee;
  }

  /**
   * @param color
   * @param title
   * @returns {*|HTMLElement}
   */
  function createLegendRow(color, title) {
    var rect ="<rect width='20' height='20' x='2' y='2' rx='5' ry='5' style='fill:COLOR;'>".replace(/COLOR/, color);
    return $("<li><svg width='25' height='25'>"+rect+"</svg>"+title+"</li>");
  }

  /**
   * Default options
   * @type {{maxMonths: number}}
   */
  $.MicaTimeline.defaultOptions = {
    maxMonths: 300
  };

}(jQuery));
