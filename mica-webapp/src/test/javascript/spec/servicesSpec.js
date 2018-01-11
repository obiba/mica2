/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

describe('Services Tests ', function () {

  beforeEach(module('mica'));

  describe('AuthenticationSharedService', function () {
    var serviceTested,
      httpBackend,
      authServiceSpied;

    beforeEach(inject(function ($httpBackend, AuthenticationSharedService, authService) {
      serviceTested = AuthenticationSharedService;
      httpBackend = $httpBackend;
      authServiceSpied = authService;
      //Request on app init
      httpBackend.expectGET('i18n/en.json').respond(200, '');
    }));
    //make sure no expectations were missed in your tests.
    //(e.g. expectGET or expectPOST)
    afterEach(function () {
      httpBackend.verifyNoOutstandingExpectation();
      httpBackend.verifyNoOutstandingRequest();
    });

    it('should call backend on logout then call authService.loginCancelled', function () {
      //GIVEN
      //set up some data for the http call to return and test later.
      var returnData = { result: 'ok' };
      //expectGET to make sure this is called once.
      httpBackend.expectGET('ws/logout').respond(returnData);

      //Set spy
      spyOn(authServiceSpied, 'loginCancelled');

      //WHEN
      serviceTested.logout();
      //flush the backend to "execute" the request to do the expectedGET assertion.
      httpBackend.flush();

      //THEN
      expect(authServiceSpied.loginCancelled).toHaveBeenCalled();
    });

  });
});


