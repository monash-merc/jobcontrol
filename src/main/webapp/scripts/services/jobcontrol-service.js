'use strict';

angular.module('jobcontrolApp')
    .service('jobcontrolEventService', function($rootScope) {
	this.triggerUpdate = function(onUpdateComplete) {
	    $rootScope.$broadcast('JOB_LIST_TRIGGER_UPDATE', onUpdateComplete);
	};
	this.listenTriggerUpdate = function(callback) {
	    $rootScope.$on('JOB_LIST_TRIGGER_UPDATE', function(event,args) { callback(args); });
	};
    })
    .factory('jobcontrolService', function($resource, $window, $q, sessionManagerService) {
	
	var executeResource = $resource('api/execute/:job', null, {
	    getJobs: { method: 'get', params: {job: 'listjobs'}, isArray: true },
	    stopJob: { method: 'get', params: {job: 'stopjob'}, isArray: true },
	    startDesktop: { method: 'get', params: {job: 'startdesktop'}, isArray: true },
	    getVncPassword: { method: 'get', params: {job: 'vncpassword'}, isArray: true },
	    getVncDisplay: { method: 'get', params: {job: 'vncdisplay'}, isArray: true },
	    getExecHost: { method: 'get', params: {job: 'exechost'}, isArray: true }
	});

	var vncTunnelResource = $resource('api/:vncRequest', null, {
	    getList: { method: 'get', params: {vncRequest: 'listvnctunnels'}, isArray: true },
	    startTunnel: { method: 'get', params: {vncRequest: 'startvnctunnel'}, isArray: false },
	    stopTunnel: { method: 'get', params: {vncRequest: 'stopvnctunnel'}, isArray: false },
	    updateVncTunnelPassword: { method: 'get', params: {vncRequest: 'updatevncpwd'}, isArray: false }
	});
	
	
	return {
	    getJobs: function(callback) {
		if (sessionManagerService.isLoggedIn()) {
		    executeResource.getJobs({}, callback);
		}
	    },
	    stopJob: function(jobId, callback) {
		if (sessionManagerService.isLoggedIn()) {
		    executeResource.stopJob({jobid: jobId}, callback);
		}
	    },
	    startDesktop: function(jobParams, callbackSuccess, callbackFail) {
		if (sessionManagerService.isLoggedIn()) {
		    executeResource.startDesktop(
			{hours: jobParams.hours,
			 minutes: jobParams.minutes,
			 nodes: jobParams.nodes,
			 ppn: jobParams.ppn,
			 queue: jobParams.queue},
			function(data) {
			    callbackSuccess(data[0].jobId);
			},
			callbackFail);
		}
	    },
	    listVncTunnels: function(callback) {
		if (sessionManagerService.isLoggedIn()) {
		    vncTunnelResource.getList({}, callback);
                }
	    },
	    startVncTunnel: function(name, password, remoteHost, display, callback) {
		var params = { 'desktopname': name,
			       'vncpassword': password,
			       'remotehost': remoteHost,
			       'display': display };
		vncTunnelResource.startTunnel(params, callback);
	    },
	    stopVncTunnel: function(id, callback) {
		vncTunnelResource.stopTunnel({id: id}, callback);
	    },
	    getVncPassword: function(jobId, callback) {
	        executeResource.getVncPassword({jobid: jobId}, function(data) { callback(data[0].password); });
	    },
	    getVncParameters: function(jobId, callback) {
		var params = { 'password': null,
                               'remoteHost': null,
			       'display': null };
		var returnValues = function() {
		    if ( params.password != null &&
			 params.remoteHost != null &&
			 params.display != null ) {
			callback(params);
		    }
		};

		executeResource.getVncPassword({jobid: jobId}, function(data) { params.password = data[0].password; returnValues(); });
		executeResource.getExecHost({jobid: jobId}, function(data) { params.remoteHost = data[0].execHost; returnValues(); });
		executeResource.getVncDisplay({jobid: jobId}, function(data) { params.display = data[0].vncDisplay; returnValues(); });
	    },
	    updateVncTunnelPassword: function(name, password, callback) {
        		var params = { 'desktopname': name,
        			       'vncpassword': password };
        		vncTunnelResource.updateVncTunnelPassword(params, callback);
        	    }
	};
    });