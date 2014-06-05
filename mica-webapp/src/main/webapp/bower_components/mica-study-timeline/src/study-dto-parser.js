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
      var bounds = findBounds(studyDto.populations);
      return parseStudy(studyDto, bounds);
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
          maxYear = Math.max(maxYear, convertToMonths(dce.endYear - startYear, dce.hasOwnProperty('endMonth') ? dce.endMonth : 12));
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
        populationData = {};
        setId(populationData, populationDto, 'id');
        setTitle(populationData, populationDto, 'name');
        populationData.color = colors.nextColor();
        if (populationDto.hasOwnProperty('dataCollectionEvents') && populationDto.dataCollectionEvents.length > 0) {
          parseEvents(populations, populationData, populationDto.dataCollectionEvents, bounds);
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

      lines.push(createPopulationItem(populationData, dto[0], bounds));

      $.each(dto, function (i, dceDto) {
        if (i === 0) return true; // first line is altreay populated
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
        if (addLine) {
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
        return convertToMonths(start - bounds.start, start > 0 ? end : 0);
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
