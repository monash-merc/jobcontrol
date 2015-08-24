'use strict';

angular.module('jobcontrolApp')
    .directive('countdown', function () {
        var convertTime = function (totalSeconds) {
            var hours = parseInt(totalSeconds / 3600) % 24;
            var minutes = parseInt(totalSeconds / 60) % 60;
            var seconds = totalSeconds % 60;
            return (hours < 10 ? "0" + hours : hours) + ":" + (minutes < 10 ? "0" + minutes : minutes) + ":" + (seconds < 10 ? "0" + seconds : seconds);
        };
        var controller = function ($scope, $interval) {
            var currentTimeInSeconds = parseInt(new Date().getTime() / 1000);
            var finishTime = parseInt($scope.startTime) + currentTimeInSeconds;
            $scope.timeRemaining = convertTime($scope.startTime);
            if (typeof $scope.countDown === 'undefined' || $scope.countDown) {
                var intervalPromise = $interval(function () {
                    var currentTimeInSeconds = parseInt(new Date().getTime() / 1000);
                    $scope.timeRemaining = convertTime(finishTime - currentTimeInSeconds);
                }, 500, $scope.startTime);
                $scope.$on('$destroy', function () {
                    $interval.cancel(intervalPromise);
                });
            }
        };

        return {
            controller: controller,
            scope: {
                startTime: '@',
                countDown: '='
            },
            template: '{{ timeRemaining }}'
        }
    })
    .directive('jobList', function () {

        var controller = function ($rootScope, $scope, $window, $q, $log, StrudelCore, ngDialog, jobcontrolEventService) {

            // Converts [dd:][hh:][mm:]ss to seconds
            var convertTime = function (timeString) {
                timeString = timeString + '';
                var timeParts = timeString.split(':').reverse();
                if (timeParts.length > 4) {
                    return undefined;
                }

                var totalSeconds = 0;
                for (var i = timeParts.length - 1; i >= 0; i--) {
                    if (i < 3) {
                        totalSeconds += parseInt(timeParts[i]) * Math.pow(60, i);
                    } else if (i === 3) {
                        totalSeconds += parseInt(timeParts[i]) * 86400;
                    }
                }
                return totalSeconds;
            };
            var cleanJobList = function(jobList) {
                angular.forEach(jobList, function (job) {
                    job.remainingWalltime = convertTime(job.remainingWalltime);
                });
                return jobList;
            };

            $scope.jobs = [];
            StrudelCore.jobControl.refreshJobList(function() {
                $scope.jobs = cleanJobList(StrudelCore.jobControl.getJobList());
                $rootScope.$apply();
            }, function(message) {
                $window.alert(message);
            });

            jobcontrolEventService.listenJobListChange(function(jobList) {
                $scope.jobs = cleanJobList(jobList);
            });

            $scope.isDesktop = function (job) {
                return job.jobName.indexOf('desktop_') === 0;
            };

            $scope.hasTunnel = function (job) {
                return StrudelCore.jobControl.jobHasTunnel(parseInt(job.jobId));
            };

            $scope.terminateJob = function (job) {
                if (job.state !== 'COMPLETING') {
                    var dialog = ngDialog.open({
                        template: 'partials/terminate-confirm.html',
                        controller: function ($scope) {
                            $scope.isProcessing = false;
                            $scope.confirm = function () {
                                $scope.isProcessing = true;
                                StrudelCore.jobControl.stopDesktop(job.jobId, function () {
                                    StrudelCore.jobControl.refreshJobList(function() {
                                        dialog.close();
                                    });
                                },
                                function(msg) {
                                    $window.alert(msg);
                                });
                            };

                            $scope.cancel = function () {
                                dialog.close();
                            }
                        }
                    });
                }
            };

            $scope.launchGuacamole = function (job) {
                var desktopName = job.jobId + '-' + job.jobName;
                if (job.state === 'RUNNING') {
                    var guacWindow = $window.open('loading-desktop.html', '_blank');
                    var hiddenIframe = document.getElementById('hiddenIframe');
                    hiddenIframe.onload = function () {
                        function waitForPasswordUpdate() {
                            $log.info('Waiting for password update');
                            var deferred = $q.defer();
                            StrudelCore.jobControl.getVncPassword(job.jobId, function (password) {
                                StrudelCore.jobControl.updateVncTunnelPassword(desktopName, password,
                                    function () {
                                        deferred.resolve();
                                    },
                                    function() {
                                        deferred.reject();
                                    });
                            },
                            function() {
                                deferred.reject();
                            });
                            return deferred.promise;
                        }

                        function waitForLogoutButton() {
                            $log.info('Simulating guacamole logout (waiting for logout button)');
                            var deferred = $q.defer();
                            var logoutButton = hiddenIframe.contentWindow.document.getElementsByClassName('logout')[0];
                            if (typeof logoutButton === 'undefined') {
                                setTimeout(waitForLogoutButton, 100);
                            } else {
                                logoutButton.click();
                                deferred.resolve();
                            }
                            return deferred.promise;
                        }

                        function waitForLogout() {
                            $log.info('Simulating guacamole logout (clicking logout button)');
                            var deferred = $q.defer();

                            function clickButton() {
                                if (hiddenIframe.contentWindow.location.hash !== '#/login/') {
                                    setTimeout(clickButton, 100);
                                } else {
                                    guacWindow.location.href = '/guacamole/#/client/c/' + desktopName;
                                    deferred.resolve();
                                }
                            }

                            clickButton();
                            return deferred.promise;
                        }
                        waitForPasswordUpdate().then(waitForLogoutButton).then(waitForLogout).then(function () {
                            delete hiddenIframe.onload;
                            $log.info('Guacamole session started');
                        });
                    };
                    document.getElementById('hiddenIframe').setAttribute('src', '/guacamole/');

                }
            };
        };

        return {
            templateUrl: 'partials/job-list.html',
            controller: controller
        };
    });
