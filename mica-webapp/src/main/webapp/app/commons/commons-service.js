/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

mica.commons
  .service('ActiveTabService', function() {

    this.getActiveTab = function(tabs) {
      if (tabs) {
        return tabs.filter(function (tab) {
          return tab.active;
        })[0];
      }

      return null;
    };

    return this;
  });
