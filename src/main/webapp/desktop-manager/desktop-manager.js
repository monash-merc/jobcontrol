'use strict';

angular.module('strudelWeb.desktop-manager', ['ngRoute', 'ngResource'])

    .config(['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/desktop-manager', {
            templateUrl: 'desktop-manager/desktop-manager.html',
            controller: 'DesktopManagerCtrl'
        });
    }])

    .controller('DesktopManagerCtrl', ['$scope', '$rootScope', '$resource', '$interval', '$location', '$mdSidenav', '$mdMedia', '$mdDialog', 'settings',
        function ($scope, $rootScope, $resource, $interval, $location, $mdSidenav, $mdMedia, $mdDialog, settings) {
            // Resources
            var configurationResource = $resource(settings.URLs.apiBase + settings.URLs.configList);
            var sessionInfoResource = $resource(settings.URLs.apiBase + settings.URLs.sessionInfo);
            var endSessionResource = $resource(settings.URLs.apiBase + settings.URLs.logout);
            var startDesktopResource = $resource(settings.URLs.apiBase + settings.URLs.startDesktop + "/in/:configuration/", {}, {
                'get': {
                    isArray: true
                }
            });
            var listDesktopsResource = $resource(settings.URLs.apiBase + settings.URLs.listDesktops + "/in/:configuration/", {}, {
                'get': {
                    isArray: true
                }
            });
            var getProjectsResource = $resource(settings.URLs.apiBase + settings.URLs.getProjects + "/in/:configuration/", {}, {
                'get': {
                    isArray: true
                }
            });
            var stopDesktopResource = $resource(settings.URLs.apiBase + settings.URLs.stopDesktop + "/in/:configuration/", {}, {
                'get': {
                    isArray: true
                }
            });
            var isDesktopRunningResource = $resource(settings.URLs.apiBase + settings.URLs.isDesktopRunning + "/in/:configuration/", {}, {
                'get': {
                    isArray: true
                }
            });
            var getMessagesFromServer = $resource(settings.URLs.apiBase + settings.URLs.messages + "/");


            // Opens or closes the left side menu
            $scope.menuToggle = function () {
                if (!$mdMedia('gt-md')) {
                    $mdSidenav("left").toggle();
                }
            };

            // Performs any initialisation after configurations
            // have been fetched from the server.
            $scope.configuration = {};
            var bootstrap = function (configuration) {
                $scope.configuration = configuration;
            };

            // Starts a new desktop
            var launchInProgress = false;
            $scope.launchDesktop = function (parameters, configuration, username) {
                launchInProgress = true;
                $rootScope.$broadcast("notify", "Starting desktop...");
                parameters.configuration = configuration.configuration.fullName;
                parameters.username = username;
                var requestParams = {
                    'configuration': parameters.configuration,
                    'username': username
                };
                if (parameters.project) {
                    requestParams['project'] = parameters.project;
                }
                listDesktopsResource.get(requestParams).$promise.then(function (desktops) {
                        if (desktops.length >= settings.maxDesktopsAllowed && settings.maxDesktopsAllowed > 0) {
                            launchInProgress = false;
                            $rootScope.$broadcast("notify", "Could not start desktop because it would exceed the limit of "
                                + settings.maxDesktopsAllowed + ((settings.maxDesktopsAllowed === 1) ? " running desktop." : " running desktops."));
                        } else {
                            startDesktopResource.get(parameters).$promise.then(
                                function (data) {
                                    $rootScope.$broadcast("notify", "Desktop #" + data[0].jobid + " launched successfully!");
                                    $scope.refreshDesktopList(configuration, username);
                                }, function (error) {
                                    $rootScope.$broadcast("notify", "Desktop failed to launch!");
                                })
                                .finally(function () {
                                    launchInProgress = false;
                                });
                        }
                    }, function (error) {
                        launchInProgress = false;
                        $rootScope.$broadcast("notify", "Desktop failed to launch! (error checking desktop list)");
                    });
            };

            $scope.desktopLaunchInProgress = function () {
                return launchInProgress;
            };

            // Stops a desktop
            var deletedDesktopIds = [];
            var doDelete = function (configuration, desktopId) {
                $rootScope.$broadcast("notify", "Deleting desktop #" + desktopId + "...");
                stopDesktopResource.get({
                    'jobidNumber': desktopId,
                    'jobid': desktopId,
                    'configuration': configuration.configuration.fullName
                }).$promise.then(
                    function (data) {
                        deletedDesktopIds.push(desktopId);
                        $rootScope.$broadcast("notify", "Desktop deleted!");
                    },
                    function (error) {
                        $rootScope.$broadcast("notify", "Could not delete desktop!");
                    }
                );
            };
            $scope.stopDesktop = function (event, configuration, desktopId) {
                var dialog = $mdDialog.confirm()
                    .title("Are you sure?")
                    .content("Are you sure you would like to delete desktop #" + desktopId + "?")
                    .targetEvent(event)
                    .ok("Yes")
                    .cancel("No");
                $mdDialog.show(dialog).then(function () {
                    doDelete(configuration, desktopId);
                });
            };

            // Refreshes the desktop list for the given configuration
            var desktopListRefreshInProgress = false;
            var desktopListRefreshPromise;
            $scope.refreshCountdown = 100;
            $scope.refreshDesktopList = function (configuration, userName) {
                function doRefresh() {
                    desktopListRefreshInProgress = true;
                    listDesktopsResource.get({
                        'configuration': configuration.configuration.fullName,
                        'username': userName
                    }).$promise.then(
                        function (data) {

                            // Check for server messages
                            getMessagesFromServer.get().$promise.then(function(serverMessages) {
                                $scope.serverMessages = [];
                                for (var key in serverMessages) {
                                    if (serverMessages.hasOwnProperty(key)) {
                                        for (var i = 0; i < serverMessages[key].length; i++) {
                                            serverMessages[key][i]['service'] = key.split("|")[0];
                                            $scope.serverMessages.push(serverMessages[key][i]);
                                        }
                                    }
                                }
                                $scope.serverMessages = $scope.serverMessages.reverse();
                            });

                            var flush = true; // This flag is used to delay the clearing of the desktops lists until the data has been fetched
                                              // because the momentary blank list looks ugly
                            if (data.length === 0) {
                                $scope.runningDesktops = [];
                                flush = false;
                            }
                            // Check the status of each desktop
                            for (var i = 0; i < data.length; i++) {
                                (function (jobData) {
                                    var id = jobData.jobid;
                                    isDesktopRunningResource.get({
                                        'configuration': configuration.configuration.fullName,
                                        'jobidNumber': id,
                                        'jobid': id
                                    }).$promise.then(function (running) {
                                            if (flush) {
                                                $scope.runningDesktops = [];
                                                flush = false;
                                            }
                                            $scope.runningDesktops.push({
                                                'jobid': jobData.jobid,
                                                'remainingWalltime': jobData.remainingWalltime,
                                                'running': (running.length === 1)
                                            });
                                        });
                                })(data[i]);
                            }
                        },
                        function (error) {
                            $rootScope.$broadcast("notify", "Could not refresh desktop list!");
                        }
                    ).finally(
                        function () {
                            desktopListRefreshInProgress = false;
                            $scope.refreshCountdown = 100;
                            desktopListRefreshPromise = $interval(function () {
                                if ($scope.refreshCountdown > 0) {
                                    $scope.refreshCountdown--;
                                }
                            }, 30, 100);
                            desktopListRefreshPromise.then(function () {
                                doRefresh();
                            });

                        }
                    );
                }

                if (angular.isDefined(desktopListRefreshPromise) && !desktopListRefreshInProgress) {
                    $interval.cancel(desktopListRefreshPromise);
                    desktopListRefreshPromise = undefined;
                }
                if (!desktopListRefreshInProgress) {
                    doRefresh();
                }
            };

            $scope.desktopListRefreshInProgress = function () {
                return desktopListRefreshInProgress;
            };

            // Stop refreshing the desktops if the route changes
            $scope.$on('$destroy', function () {
                if (desktopListRefreshPromise) {
                    $interval.cancel(desktopListRefreshPromise);
                }
            });

            // Redirects to the desktop viewer
            $scope.showDesktop = function (configuration, jobId) {
                $location.path("/desktop-viewer/" + configuration.configuration.fullName + "/" + jobId);
            };

            // Sets the selection as selected, and sets new desktop parameters
            // to their defaults.
            $scope.setSelectedConfiguration = function (name, configuration) {
                if (angular.isDefined($scope.selectedConfiguration) && $scope.selectedConfiguration.name === name) {
                    return;
                }
                launchInProgress = false;
                $scope.selectedConfiguration = {
                    name: name,
                    configuration: configuration
                };
                $scope.newDesktop = {
                    nodes: 1,
                    hours: parseInt(configuration.startserver.defaultParams.hours),
                    ppn: parseInt(configuration.startserver.defaultParams.ppn),
                    mem: parseInt(configuration.startserver.defaultParams.mem),
                    resolution: "1440x900"
                };
                if (configuration.projects) {
                    $scope.newDesktop['project'] = configuration.projects[0];
                }
            };

            // For a given authBackendName, filter all accessible configurations
            var getAccessibleConfigurations = function (authBackendName, configurationData) {
                var configs = {};
                for (var i in configurationData) {
                    if (configurationData.hasOwnProperty(i) && configurationData[i].hasOwnProperty("authBackendNames")) {
                        var c = configurationData[i];
                        if (c.authBackendNames.indexOf(authBackendName) > -1) {
                            var facilityAndFlavour = i.split("|");
                            if (!configs.hasOwnProperty(facilityAndFlavour[0])) {
                                configs[facilityAndFlavour[0]] = {};
                            }
                            c.configurations.fullName = i;

                            // Get available projects (if any) for a configuration
                            (function(config) {
                                // Gets a list of projects if supported by backend
                                getProjectsResource.get({
                                    'configuration': config.fullName
                                }).$promise.then(function (projects) {
                                    config.projects = [];
                                    for (var i = 0; i < projects.length; i++) {
                                        var project = projects[i].group;
                                        if (project !== "") {
                                            config.projects.push(project);
                                        }
                                    }
                                });
                            })(c.configurations);

                            configs[facilityAndFlavour[0]][facilityAndFlavour[1]] = c.configurations;
                        }
                    }
                }

                return configs;
            };

            // Gets the session data and redirects to the login screen if the user is not logged in
            sessionInfoResource.get({}).$promise.then(function (sessionData) {
                if (sessionData.has_certificate !== "true") {
                    $location.path("/system-selector");
                    return;
                }
                $scope.session = sessionData;
                configurationResource.get({}).$promise.then(function (configurationData) {
                    bootstrap(getAccessibleConfigurations(sessionData.auth_backend_name, configurationData));
                })
            });

            // Signs out the current user and redirects to the login screen
            $scope.doSignout = function () {
                endSessionResource.get({}, function () {
                    $location.path("/system-selector");
                });
            };

        }]);