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
