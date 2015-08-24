'use strict';

angular.module('jobcontrolApp')
    .controller('LoginCtrl', function($scope, sessionManagerService, sessionEventService) {
        $scope.doLogin = sessionManagerService.doLogin;

        $scope.loginInProgress = false;
        sessionEventService.listenLoginInProgress(function() {
            $scope.loginInProgress = true;
        });
        sessionEventService.listenLoginSuccess(function() {
            $scope.loginInProgress = false;
        });
        sessionEventService.listenLogout(function() {
            $scope.loginInProgress = false;
        })
    });