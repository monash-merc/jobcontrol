'use strict';

angular.module('jobcontrolApp')
	.controller('MainCtrl', function($scope, $interval, ngDialog, sessionManagerService, sessionEventService, jobcontrolService, jobcontrolEventService) {
		$scope.doLogin = sessionManagerService.doLogin;
		$scope.doLogout = sessionManagerService.doLogout;
		$scope.isLoggedIn = sessionManagerService.isLoggedIn;
		$scope.loginStatus = sessionManagerService.getLoginStatus;
		$scope.currentUser = sessionManagerService.getCurrentState;
		
		$scope.launchDesktop = function() {
			var dialog = ngDialog.open({
				template: 'partials/desktop-launcher.html',
				controller: function($scope) {
					$scope.job = {
							hours: '01',
							minutes: '00',
							nodes: '1',
							ppn: '1'
								};
					$scope.parseInt = parseInt;
					$scope.fmtTimeHrs = function(number) {
						var n = parseInt(number);
						if (n < 10) {
							return '0'+n;
						} else {
							return n;
						}
					};
					$scope.fmtTimeMins = function(number) {
						var n = parseInt(number);
						if (n < 10) {
							return '0'+n;
						} else if (n > 59) {
							return 59;
						} else {
							return n;
						}
					};
					
					$scope.error = false;
					$scope.isProcessing = false;
					$scope.launch = function(job) {
						$scope.error = false;
						$scope.isProcessing = true;
						jobcontrolService.startDesktop(job, function(jobId) {
							jobcontrolEventService.triggerUpdate(function() {
								$scope.isProcessing = false;
								dialog.close();
							});
						}, function() {
							$scope.isProcessing = false;
							$scope.error = true;
						});
					};
					
					$scope.cancel = function() {
						dialog.close();
					}
				}
			});
		};
	});