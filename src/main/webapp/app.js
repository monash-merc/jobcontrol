'use strict';

// Declare app level module which depends on views, and components
angular.module('strudelWeb', [
    'ngMaterial',
    'ngRoute',
    'strudelWeb.system-selector',
    'strudelWeb.desktop-manager',
    'strudelWeb.desktop-viewer',
    'strudelWeb.version'
]).
    config(['$routeProvider', '$httpProvider',
        function ($routeProvider, $httpProvider) {
            $routeProvider.otherwise({redirectTo: '/system-selector'});
            $httpProvider.interceptors.push('APIInterceptor');
        }])
    .constant('settings', {
        'URLs': {
            'base': '/strudel-web/',
            'apiBase': '/strudel-web/api/',
            'configList': 'configurations',
            'oauthStart': 'login',
            'logout': 'end_session',
            'sessionInfo': 'session_info',
            'registerKey': 'register_key',
            'getProjects': 'execute/getprojects',
            'startDesktop': 'execute/startserver',
            'stopDesktop': 'execute/stop',
            'listDesktops': 'execute/listall',
            'execHost': 'execute/exechost',
            'listVncTunnels': 'listvnctunnels',
            'getVncDisplay': 'execute/vncdisplay',
            'startVncTunnel': 'startvnctunnel',
            'oneTimePassword': 'execute/otp',
            'updateVncPassword': 'updatevncpwd',
            'isDesktopRunning': 'execute/running',
            'guacamole': '/guacamole/',
            'messages': 'messages'
        },
        'maxDesktopsAllowed': 1,
        'maxRetryOnServerError': 5
    })
    .service('APIInterceptor', ['$rootScope', '$location', '$injector', '$timeout', '$log', 'settings',
        function ($rootScope, $location, $injector, $timeout, $log, settings) {
            var service = this;

            // Keep track of the failures per URL
            var failures = {};

            service.request = function (config) {
                return config;
            };

            service.response = function (response) {
                if (response.status !== 500 && failures.hasOwnProperty(response.config.url)) {
                    delete failures[response.config.url];
                }
                return response;
            };

            service.responseError = function (response) {
                // Redirect to login page on unauthorised response
                if (response.status === 403) {
                    $rootScope.$broadcast("notify", "You've been logged out!");
                    $location.path('/');

                // Retry on 500
                } else if (response.status === 500) {
                    var url = response.config.url;
                    if (failures.hasOwnProperty(url)) {
                        failures[url]++;
                    } else {
                        failures[url] = 1;
                    }
                    if (failures[url] < settings.maxRetryOnServerError) {
                        $log.error("Retrying failed request (500) for URL " + url + " (" + failures[url] + "failures so far)");
                        return $timeout(function () {
                            var $http = $injector.get('$http');
                            return $http(response.config);
                        }, 3000);
                    }
                }
                return response;
            };
        }])
    .controller('AppCtrl', ['$mdToast', '$rootScope', '$scope', function ($mdToast, $rootScope, $scope) {
        $rootScope.$on('notify', function (event, message) {
            console.log(message);
            $mdToast.show(
                $mdToast.simple()
                    .content(message)
                    .position("bottom right")
                    .hideDelay(10000)
            );
        });

        var hideFeedbackButton = function() {
            var buttonElements = document.getElementsByClassName("feedback-btn");
            if (buttonElements.length > 0) {
                buttonElements[0].style.display = 'none';
            }
        };
        var showFeedbackButton = function() {
            var buttonElements = document.getElementsByClassName("feedback-btn");
            if (buttonElements.length > 0) {
                buttonElements[0].style.display = 'block';
            }
        };
        $scope.toolbarHidden = false;
        $rootScope.$on('makeToolbarVisible', function (event) {
            $scope.toolbarHidden = false;
            showFeedbackButton();
        });

        $rootScope.$on('makeToolbarInvisible', function (event) {
            $scope.toolbarHidden = true;
            hideFeedbackButton();
        });
        $rootScope.$on('$routeChangeSuccess', function (event) {
            $scope.toolbarHidden = false;
            showFeedbackButton();
        });
    }]);
