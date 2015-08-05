'use strict';

angular.module('jobcontrolApp')
    .config(function($stateProvider, $urlRouterProvider) {

        $urlRouterProvider.otherwise('/');

        $stateProvider
            .state('login', {
                url: '/login',
                templateUrl: 'views/login/templates/login.html'
            })
            .state('home', {
                url: '/',
                templateUrl: 'views/desktop-list/templates/desktop-list.html'
            });
    })
    .run(function($rootScope, $state, sessionManagerService, $log) {
        $rootScope.$on('$stateChangeStart', function(e, toState, toParams, fromState, fromParams) {
            if (toState.name === "login" && sessionManagerService.isLoggedIn()) {
                e.preventDefault();
                $state.go("home");
                return;
            } else if (toState.name === "login") {
                return;
            } else if (!sessionManagerService.isLoggedIn()) {
                e.preventDefault();
                $state.go("login");
                return;
            }
        });
    });