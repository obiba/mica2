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