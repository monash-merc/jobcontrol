'use strict';

angular.module('jobcontrolApp')
    .service('sessionEventService', function($rootScope) {
	this.broadcastLoginSuccess = function(userData) { $rootScope.$broadcast('LOGIN_SUCCESS_EVENT', userData); }
	this.listenLoginSuccess = function(callback) { $rootScope.$on('LOGIN_SUCCESS_EVENT', function(event,args) { callback(args); }); }
	
	this.broadcastLogout = function() { $rootScope.$broadcast('LOGOUT_EVENT'); }
	this.listenLogout = function(callback) { $rootScope.$on('LOGOUT_EVENT', callback); }
    })
    .factory('sessionManagerService', function($resource, $window, $q, $interval, sessionEventService) {
	
	// Resources
	var sessionInfoResource = $resource('api/session_info', null, {
	    getSessionInfo: { method: 'get' }
	});
	var keyRegistration = $resource('api/register_key', null, {
	    registerKey: { method: 'get' }
	});
	var logout = $resource('api/end_session', null, {
	    doLogout: { method: 'get' }
	})
	
	// Maintain the current state
	var currentUserState = {};
	var updateUserState = function() {
	    var deferred = $q.defer();
	    sessionInfoResource.getSessionInfo(function(userData) {
		currentUserState = userData;
		deferred.resolve(userData);
	    });
	    return deferred.promise;
	}
	updateUserState();
	
	
	
	sessionEventService.listenLoginSuccess(function(userData) { currentUserState = userData; });
	sessionEventService.listenLogout(function() { updateUserState(); });
	
	// Actions
	var getToken = function() {
	    if (typeof currentUserState.session_id !== 'undefined') {
		return currentUserState.session_id;
	    } else {
		return null;
	    }
	}
	
	var loginStatus = null;
	var doLogin = function() {
	    $window.loginComplete = function() {
		loginStatus = "Authorising keys with remote host... Please wait.";
		keyRegistration.registerKey({}, function() {
		    sessionInfoResource.getSessionInfo(function(data) {
			loginStatus = null;
			sessionEventService.broadcastLoginSuccess(data);
		    });
		});
		
	    };
	    
	    // Workaround for popup blocker (open window first, set href after async request)
	    var w = $window.open("about:blank", "_blank", "width=900, height=500");
	    // Always update the state -- token could have expired
	    updateUserState().then(function(userData) {
		w.location.href = "login?token="+userData.session_id;
	    });
	}
	var doLogout = function() {
	    logout.doLogout({}, function() { sessionEventService.broadcastLogout(); });
	}
	
	var isLoggedIn = function() {
	    if (typeof currentUserState.uid === 'undefined' || currentUserState.uid === '') {
		return false;
	    } else if (currentUserState.has_certificate === 'false') {
		return false;
	    } else {
		return true;
	    }
	}
	// Automatically log out if the session is invalidated on the server side
	$interval(function() {
	    if (isLoggedIn()) {
		updateUserState();
	    }
	}, 5000);
	
	return {
	    getSessionInfo: sessionInfoResource.getSessionInfo,
	    doLogin: doLogin,
	    doLogout: doLogout,
	    getCurrentState: function() { return currentUserState; },
	    getLoginStatus: function() { return loginStatus; },
	    isLoggedIn: isLoggedIn,
	    getToken: getToken
	};
	
    });