'use strict';

angular.module('strudelWeb.system-selector', ['ngRoute', 'ngResource'])

    .config(['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/system-selector', {
            templateUrl: 'system-selector/system-selector.html',
            controller: 'SystemSelectorCtrl'
        });
    }])

    .controller('SystemSelectorCtrl', ['$scope', '$rootScope', '$window', '$resource', '$mdMedia', '$interval', '$location', 'settings',
        function ($scope, $rootScope, $window, $resource, $mdMedia, $interval, $location, settings) {
            // Resources
            var configurationResource = $resource(settings.URLs.apiBase + settings.URLs.configList);
            var sessionInfoResource = $resource(settings.URLs.apiBase + settings.URLs.sessionInfo);
            var endSessionResource = $resource(settings.URLs.apiBase + settings.URLs.logout);
            var certificateRegistrationResource = $resource(settings.URLs.apiBase + settings.URLs.registerKey);

            // Creates a set of auth backends to present to the user
            $scope.authBackends = [];
            configurationResource.get({}, function (data) {
                for (var configKey in data) {
                    if (data.hasOwnProperty(configKey)) {
                        for (var authBackendKey in data[configKey].authBackendNames) {
                            if (data[configKey].authBackendNames.hasOwnProperty(authBackendKey)) {
                                var backendName = data[configKey].authBackendNames[authBackendKey];
                                if ($scope.authBackends.indexOf(backendName) === -1) {
                                    $scope.authBackends.push(backendName);
                                }
                            }
                        }
                    }
                }
            });

            // Starts the login auth flow
            var loginWindow;
            $scope.startLogin = function (service) {

                var width = 800,
                    height = 600;
                var left = screen.width/2 - width/2,
                    top = screen.height/2 - height/2;

                var url = settings.URLs.base + settings.URLs.oauthStart + "?service="+service;
                loginWindow = $window.open('about:blank', '', "top=" + top + ",left=" + left + ",width="+width+",height="+height);

                // End any existing sessions before starting a new one
                endSessionResource.get({}, function() {
                    loginWindow.location = url;
                });
            };

            var certificateRequestInProgress = false;
            var getSSHCertificate = function() {
                certificateRegistrationResource.get({}, function(data) {
                    if (data.status !== "OK") {
                        $rootScope.$broadcast("notify", "Access not granted - contact your system administrator.");
                    } else {
                        certificateRequestInProgress = false;
                        $rootScope.$broadcast("notify", "Access granted!");
                        $location.path("/desktop-manager");
                    }
                });
            };

            // Called any time the login popup closes
            var onLoginWindowClose = function() {
                sessionInfoResource.get({}, function(data) {
                    if (data.has_oauth_access_token === "true") {
                        $rootScope.$broadcast("notify", "User name and password correct. Requesting access to remote system...");
                        certificateRequestInProgress = true;
                        getSSHCertificate();
                    } else {
                        $rootScope.$broadcast("notify", "Login failed :(");
                    }
                });
            };

            // Detects whether the login window is still open
            var loginWindowPreviouslyClosed = true;
            var loginWindowOpen = function() {
                var windowOpen = angular.isDefined(loginWindow) && loginWindow.closed === false;
                if (loginWindowPreviouslyClosed === false && windowOpen === false) {
                    loginWindowPreviouslyClosed = true;
                    onLoginWindowClose();
                } else if (loginWindowPreviouslyClosed === true && windowOpen === true) {
                    loginWindowPreviouslyClosed = false;
                }
                return windowOpen;
            };
            $interval(loginWindowOpen, 500); // Don't rely on the digest cycle - it can be slow.

            $scope.showSpinner = function() {
                return loginWindowOpen() || certificateRequestInProgress;
            };

            // Disables the login button if the service is not selected, or a login is currently in progress
            $scope.loginButtonDisabled = function(service) {
                return !angular.isDefined(service) || loginWindowOpen() || certificateRequestInProgress;
            };

        }]);