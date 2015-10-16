'use strict';

angular.module('strudelWeb.desktop-viewer', ['ngRoute', 'ngResource'])

    .config(['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/desktop-viewer/:configuration/:desktopId/', {
            templateUrl: 'desktop-viewer/desktop-viewer.html',
            controller: 'DesktopViewerCtrl'
        });
    }])

    .controller('DesktopViewerCtrl', ['$scope', '$rootScope', '$resource', '$location', '$routeParams', '$interval', '$sce', 'settings',
        function ($scope, $rootScope, $resource, $location, $routeParams, $interval, $sce, settings) {
            // Resources
            var sessionInfoResource = $resource(settings.URLs.apiBase + settings.URLs.sessionInfo);
            var listVncTunnelsResource = $resource(settings.URLs.apiBase + settings.URLs.listVncTunnels, {}, {
                'get': {
                    isArray: true
                }
            });
            var execHostResource = $resource(settings.URLs.apiBase + settings.URLs.execHost + "/in/:configuration/", {}, {
                'get': {
                    isArray: true
                }
            });
            var vncDisplayResource = $resource(settings.URLs.apiBase + settings.URLs.getVncDisplay + "/in/:configuration/on/:host/", {}, {
                'get': {
                    isArray: true
                }
            });
            var startVncTunnelResource = $resource(settings.URLs.apiBase + settings.URLs.startVncTunnel);
            var oneTimePasswordResource = $resource(settings.URLs.apiBase + settings.URLs.oneTimePassword + "/in/:configuration/on/:host/", {}, {
                'get': {
                    isArray: true
                }
            });
            var updateVncPasswordResource = $resource(settings.URLs.apiBase + settings.URLs.updateVncPassword);

            $scope.guacamoleUrl = $sce.trustAsResourceUrl(settings.URLs.guacamole);

            var generateTunnelName = function (userName, configurationName, desktopId) {
                return (userName + '-' + configurationName + '-' + desktopId).replace("|", "-").replace(" ", "_");
            };

            $scope.desktopReady = false;
            var bootstrap = function (userName, configurationName, desktopId) {
                // 1. Get the execution host of the desktop
                execHostResource.get({
                    'jobidNumber': desktopId,
                    'configuration': configurationName
                }).$promise
                    .then(function (data) {
                        return data[0].execHost;
                    },
                    function (error) {
                        $rootScope.$broadcast("notify", "Could not get the execution host for this desktop!");
                    })
                    // 2. Get the display number
                    .then(function (host) {
                        return vncDisplayResource.get({
                            'configuration': configurationName,
                            'jobidNumber': desktopId,
                            'host': host
                        }).$promise
                            .then(function (data) {
                                return {
                                    'host': host,
                                    'display': data[0].vncDisplay
                                };
                            },
                            function (error) {
                                $rootScope.$broadcast("notify", "Could not get the display number for this desktop!");
                            }
                        );
                    })
                    // 3. Generate one-time password
                    .then(function (vncInfo) {
                        return oneTimePasswordResource.get({
                            'vncDisplay': vncInfo.display,
                            'configuration': configurationName,
                            'host': vncInfo.host
                        }).$promise.then(function (data) {
                                vncInfo.password = data[0].vncPasswd;
                                console.log(vncInfo);
                                return vncInfo;
                            },
                            function (error) {
                                $rootScope.$broadcast("notify", "Could not generate a one-time password for this desktop!");
                            });
                    })
                    // 4. Check existing tunnels
                    .then(function (vncInfo) {
                        return listVncTunnelsResource.get({}).$promise.then(function (tunnels) {
                                var hasTunnel = false;
                                for (var i = 0; i < tunnels.length; i++) {
                                    if (tunnels[i].desktopName === generateTunnelName(userName, configurationName, desktopId)) {
                                        hasTunnel = true;
                                        break;
                                    }
                                }
                                if (hasTunnel) {
                                    // 4a. Update password
                                    return updateVncPasswordResource.get({
                                        'desktopname': generateTunnelName(userName, configurationName, desktopId),
                                        'vncpassword': vncInfo.password
                                    }).$promise
                                        .then(function (data) {
                                            console.log(data);
                                            return generateTunnelName(userName, configurationName, desktopId);
                                        },
                                        function () {
                                            $rootScope.$broadcast("notify", "Could not update the VNC one-time password!");
                                        });
                                } else {
                                    // 4b. Create tunnel
                                    return startVncTunnelResource.get({
                                        'desktopname': generateTunnelName(userName, configurationName, desktopId),
                                        'vncpassword': vncInfo.password,
                                        'remotehost': vncInfo.host,
                                        'display': vncInfo.display.replace(":", "")
                                    }).$promise
                                        .then(function () {
                                            return generateTunnelName(userName, configurationName, desktopId);
                                        },
                                        function () {
                                            $rootScope.$broadcast("notify", "Could not start the VNC tunnel!");
                                        });
                                }
                            },
                            function (error) {
                                $rootScope.$broadcast("notify", "Could not get the list of running tunnels!");
                            });
                    })
                    // Refresh Guacamole
                    .then(function(desktopName) {
                        var guacamoleFrame = document.getElementById("guacamoleFrame");
                        var guacamoleContent = guacamoleFrame.contentDocument || guacamoleFrame.contentWindow.document;
                        guacamoleContent.getElementsByClassName("logout")[0].click();
                        var intervalPromise;
                        intervalPromise = $interval(function() {
                            if ( guacamoleContent.location.hash === "#/login/") {
                                console.log(desktopName);
                                guacamoleFrame.setAttribute("src", $scope.guacamoleUrl+"#/client/c/"+desktopName);
                                $scope.desktopReady = true;
                                $interval.cancel(intervalPromise);
                                $rootScope.$broadcast("makeToolbarInvisible");
                            }
                        }, 100);
                    });
            };

            // Gets the session data and redirects to the login screen if the user is not logged in
            sessionInfoResource.get({}).$promise.then(function (sessionData) {
                if (sessionData.has_certificate !== "true") {
                    $location.path("/system-selector");
                    return;
                }
                bootstrap(sessionData.uid, $routeParams.configuration, $routeParams.desktopId);
            });


        }]);