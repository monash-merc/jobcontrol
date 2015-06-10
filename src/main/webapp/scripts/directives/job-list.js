'use strict';

angular.module('jobcontrolApp')
    .directive('countdown', function() {
	var convertTime =  function(totalSeconds) {
	    var hours = parseInt(totalSeconds / 3600 ) % 24;
	    var minutes = parseInt( totalSeconds / 60 ) % 60;
	    var seconds = totalSeconds % 60;
	    return (hours < 10 ? "0" + hours : hours) + ":" + (minutes < 10 ? "0" + minutes : minutes) + ":" + (seconds  < 10 ? "0" + seconds : seconds);
	}
	var controller = function($scope, $interval) {
	    var currentTimeInSeconds = parseInt(new Date().getTime() / 1000);
	    var finishTime = parseInt($scope.startTime) + currentTimeInSeconds;
	    $scope.timeRemaining = convertTime($scope.startTime);
	    if (typeof $scope.countDown === 'undefined' || $scope.countDown) {
		var intervalPromise = $interval(function() {
		    var currentTimeInSeconds = parseInt(new Date().getTime() / 1000);
		    $scope.timeRemaining = convertTime(finishTime - currentTimeInSeconds);
		}, 500, $scope.startTime);
		$scope.$on('$destroy', function() { $interval.cancel(intervalPromise); });
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
    .directive('jobList', function() {
	
	var controller = function($scope, $interval, $window, $q, $log, ngDialog, jobcontrolService, jobcontrolEventService) {
	    
	    $scope.jobsLoading = true;
	    $scope.jobs = [];
	    $scope.tunnelList = [];

	    $scope.isDesktop = function(job) {
		return job.jobName.indexOf('desktop_') === 0;
	    };

	    $scope.hasTunnel = function(job) {
		var prefix = job.jobId + '-desktop_';
		for (var i = 0; i < $scope.tunnelList.length; i ++) {
		    if ($scope.tunnelList[i].desktopName.indexOf(prefix) === 0) {
			return true;
		    }
		}
		return false;
	    };
	    
	    // Converts [dd:][hh:][mm:]ss to seconds
	    var convertTime = function(timeString) {
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
	    }
	    
	    var refreshJobs = function(callback) {
		$scope.jobsLoading = true;
		jobcontrolService.getJobs(function(jobs) {
		    angular.forEach(jobs, function(value,key,obj) {
			value.remainingWalltime = convertTime(value.remainingWalltime);
		    });
		    $scope.jobs = jobs;
		    $scope.jobsLoading = false;
		    
		    if (typeof callback === 'function') {
			callback(jobs);
		    }
		});
	    };

	    var setupTunnel = function(job) {
	    var deferred = $q.defer();
		if (job.state === 'RUNNING' && $scope.isDesktop(job) && !$scope.hasTunnel(job)) {
		    var desktopName = job.jobId + '-'+job.jobName;
		    jobcontrolService.getVncParameters(job.jobId, function(params) {
			jobcontrolService.startVncTunnel(desktopName, params.password, params.remoteHost, params.display, function() {
				deferred.resolve();
			});
		    });
		} else {
			deferred.resolve();
		}
		return deferred.promise;
	    }

		var refreshTunnelLock = false;
	    var refreshTunnelList = function(jobs) {
		jobcontrolService.listVncTunnels(function(tunnelList) {
		    $scope.tunnelList = tunnelList;
		    // Look for stale tunnels
		    var tunnelJobIdRegex = /^([0-9]+)/;
		    for (var i = 0; i < tunnelList.length; i++) {
			var jobId = tunnelJobIdRegex.exec(tunnelList[i].desktopName);
			if (jobId != null) {
			    jobId = parseInt(jobId[0]);
			    var isStale = true;
			    for (var j = 0; j < jobs.length; j++) {
				if (jobId === parseInt(jobs[j].jobId)) {
				    isStale = false;
				    break;
				}
			    }
			    if (isStale) {
				jobcontrolService.stopVncTunnel(tunnelList[i].id);
			    }
			}
		    }
		    // Create new tunnels
		    if (!refreshTunnelLock) {
		    	$log.info('Performing tunnel kickoff');
		    	refreshTunnelLock = true;
		    	var tunnelPromise = null;
		    	$log.info($scope.tunnelList);
		    	for (var i = 0; i < jobs.length; i++) {
		    		if (tunnelPromise === null) {
						tunnelPromise = setupTunnel(jobs[i]);
					} else {
						tunnelPromise = tunnelPromise.then(function() {
							return setupTunnel(jobs[i]);
						});
					}
				}
		    	if (tunnelPromise !== null) {
		    		tunnelPromise.finally(function() {
		    			$log.info('Tunnel refresh complete, releasing lock');
						refreshTunnelLock = false;
		    		});
		    	} else {
		    		$log.info('No tunnels need to be created, releasing lock');
		    		refreshTunnelLock = false;
		    	}
		    } else {
		    	$log.info('Skipping tunnel kickoff - already in progress');
		    }
		});
	    };
	    
	    refreshJobs(refreshTunnelList);
	    $interval(function() { refreshJobs(refreshTunnelList); }, 5000);
	    jobcontrolEventService.listenTriggerUpdate(function(callback) { refreshJobs(callback); });
	    
	    $scope.terminateJob = function(job) {
		if (job.state !== 'COMPLETING') {
		    var dialog = ngDialog.open({
			template: 'partials/terminate-confirm.html',
			controller: function($scope) {
			    $scope.isProcessing = false;
			    $scope.confirm = function() {
				$scope.isProcessing = true;
				jobcontrolService.stopJob(job.jobId, function() {
				    refreshJobs(function () {
					dialog.close();
				    });
				});
			    };
			    
			    $scope.cancel = function() {
				dialog.close();
			    }
			}
		    });
		}
	    };

	    $scope.launchGuacamole = function(job) {
	    var desktopName = job.jobId+'-'+job.jobName;
		if (job.state === 'RUNNING') {
		    var guacWindow = $window.open('loading-desktop.html', '_blank');
		    var hiddenIframe = document.getElementById('hiddenIframe');
		    var onLoadHandler = function() {
		    function waitForPasswordUpdate() {
			$log.info('Waiting for password update');
			var deferred = $q.defer();
		    	jobcontrolService.getVncPassword(job.jobId, function(password) {
		    		jobcontrolService.updateVncTunnelPassword(desktopName, password,
		    		function() {
				    deferred.resolve();
		    		});
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
				    guacWindow.location.href='/guacamole-0.9.5/#/client/c/'+desktopName;
				    deferred.resolve();
				}
			    }
			    clickButton();
			    return deferred.promise;
			};
			waitForPasswordUpdate().then(waitForLogoutButton).then(waitForLogout).then(function() {
			    delete hiddenIframe.onload;
			    $log.info('Guacamole session started');
			});
		    };
		    hiddenIframe.onload = onLoadHandler;
		    document.getElementById('hiddenIframe').setAttribute('src', '/guacamole-0.9.5');

		}
	    };
	};
	
	return {
	    templateUrl: 'partials/job-list.html',
	    controller: controller
	};
    });
