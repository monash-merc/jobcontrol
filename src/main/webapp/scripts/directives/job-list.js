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
	
	var controller = function($scope, $interval, $window, ngDialog, jobcontrolService, jobcontrolEventService) {
	    
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
			callback();
		    }
		});
	    };

	    var setupTunnel = function(job) {
		if (job.state === 'RUNNING' && $scope.isDesktop(job) && !$scope.hasTunnel(job)) {
		    var desktopName = job.jobId + '-'+job.jobName;
		    jobcontrolService.getVncParameters(job.jobId, function(params) {
			jobcontrolService.startVncTunnel(desktopName, params.password, params.remoteHost, params.display);
		    });
		}
	    }

	    var refreshTunnelList = function() {
		jobcontrolService.listVncTunnels(function(tunnelList) {
		    $scope.tunnelList = tunnelList;
		    // Look for stale tunnels
		    var tunnelJobIdRegex = /^([0-9]+)/;
		    for (var i = 0; i < $scope.tunnelList.length; i++) {
			var jobId = tunnelJobIdRegex.exec($scope.tunnelList[i].desktopName);
			if (jobId != null) {
			    jobId = parseInt(jobId[0]);
			    var isStale = true;
			    for (var j = 0; j < $scope.jobs.length; j++) {
				if (jobId === parseInt($scope.jobs[j].jobId)) {
				    isStale = false;
				    break;
				}
			    }
			    if (isStale) {
				jobcontrolService.stopVncTunnel($scope.tunnelList[i].id);
			    }
			}
		    }
		    // Create new tunnels
		    for (var i = 0; i < $scope.jobs.length; i++) {
			setupTunnel($scope.jobs[i]);
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
		    	var done = false;
		    	jobcontrolService.getVncPassword(job.jobId, function(password) {
		    		jobcontrolService.updateVncTunnelPassword(desktopName, password,
		    		function() {
		    			done = true;
		    		});
		    	})

		    	function wait() {
		    		if (!done) {
		    			setTimeout(wait, 100);
		    		}
		    	};
		    	wait();
		    }
			function waitForLogoutButton() {
			    var logoutButton = hiddenIframe.contentWindow.document.getElementsByClassName('logout')[0];
			    if (typeof logoutButton === 'undefined') {
				setTimeout(waitForLogoutButton, 100);
			    } else {
				logoutButton.click();
			    }
			}
			function waitForLogout() {
			    console.log(hiddenIframe.contentWindow.location.hash);
			    if (hiddenIframe.contentWindow.location.hash !== '#/login/') {
				setTimeout(waitForLogout, 100);
			    } else {
				guacWindow.location.href='/guacamole-0.9.5/#/client/c/'+desktopName;
			    }
			};
			waitForPasswordUpdate();
			waitForLogoutButton();
			waitForLogout();
			delete hiddenIframe.onload;
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
