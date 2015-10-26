'use strict';

angular.module('strudelWeb.desktop-viewer', ['ngRoute', 'ngResource', 'ngCookies'])

    .config(['$cookiesProvider', function ($cookiesProvider) {
        $cookiesProvider.defaults.path = '/';
    }])

    .config(['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/desktop-viewer/:configuration/:desktopId/', {
            templateUrl: 'desktop-viewer/desktop-viewer.html',
            controller: 'DesktopViewerCtrl'
        });
    }])

    .controller('DesktopViewerCtrl',
    ['$scope', '$rootScope', '$cookies', '$http', '$resource', '$location', '$routeParams', '$sce', 'settings',
        function ($scope, $rootScope, $cookies, $http, $resource, $location, $routeParams, $sce, settings) {
            // Resources
            var sessionInfoResource = $resource(settings.URLs.apiBase + settings.URLs.sessionInfo);
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

            $scope.guacamoleUrl = $sce.trustAsResourceUrl(settings.URLs.guacamole);

            $scope.desktopReady = false;
            var bootstrap = function (userName, configurationName, desktopId) {
                var desktopName = "desktop" + Date.now();

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
                                    'desktopName': desktopName,
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
                        return startVncTunnelResource.get({
                            'desktopname': vncInfo.desktopName,
                            'vncpassword': vncInfo.password,
                            'remotehost': vncInfo.host,
                            'display': vncInfo.display.replace(":", ""),
                            'configuration': configurationName
                        }).$promise
                            .then(function (newTunnel) {
                                vncInfo.port = newTunnel.localPort;
                                return vncInfo;
                            },
                            function () {
                                $rootScope.$broadcast("notify", "Could not start the VNC tunnel!");
                            });
                    })
                    // Refresh Guacamole
                    .then(function (vncInfo) {
                        var guacamoleFrame = document.getElementById("guacamoleFrame");
                        var guacamoleContent = guacamoleFrame.contentDocument || guacamoleFrame.contentWindow.document;

                        // Get the GUAC_AUTH cookie
                        var guacAuthCookie = (function (cookies) {
                            for (var i = 0; i < cookies.length; i++) {
                                if (cookies[i].startsWith("GUAC_AUTH=")) {
                                    return JSON.parse(decodeURIComponent(cookies[i].split("=")[1]));
                                }
                            }
                            return null;
                        })(guacamoleContent.cookie.split(";"));

                        function redirectGuacIframe() {
                            guacamoleContent.location.hash = "#/client/c/" + vncInfo.desktopName;
                            $rootScope.$broadcast("makeToolbarInvisible");
                            $scope.desktopReady = true;
                        }

                        // This is the cookie that Guacamole will intercept for connection credentials
                        // The auth plugin for guacamole inspects any cookie beginning with "vnc-credentials"
                        var cookieExpiry = new Date();
                        cookieExpiry.setTime(cookieExpiry.getTime() + (1 * 60 * 1000)); // 1 minute expiry
                        $cookies.put("vnc-credentials-" + Date.now().toString(), JSON.stringify(
                                {
                                    'name': vncInfo.desktopName,
                                    'hostname': 'localhost',
                                    'port': vncInfo.port.toString(),
                                    'password': vncInfo.password,
                                    'protocol': 'vnc'
                                }
                            ),
                            {
                                'expires': cookieExpiry
                            });

                        if (guacAuthCookie) {
                            // Invalidate guacamole's auth token, redirect the iframe to the new desktop
                            $http.delete($scope.guacamoleUrl + "api/tokens/" + guacAuthCookie.authToken)
                                .then(redirectGuacIframe);
                        } else {
                            redirectGuacIframe();
                        }
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