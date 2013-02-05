'use strict';

describe('Common: Navigation', function () {

  // load the controller's module
  beforeEach(module('app.common.navigation'));

  describe('Common: Navigation', function () {

    var NavigationController, scope;

    // Initialize the controller and a mock scope
    beforeEach(inject(function ($controller) {
      scope = {};
      NavigationController = $controller('NavigationController', {
        $scope: scope
      });
    }));

    it('should attach a list of navigation to the scope', function () {
      expect(scope.navigation.length).toBe(7);
    });
  });
});
