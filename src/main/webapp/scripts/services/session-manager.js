'use strict';

angular.module('jobcontrolApp')
    .service('sessionEventService', function ($rootScope) {
        this.broadcastLoginInProgress = function (userData) {
            $rootScope.$broadcast('LOGIN_IN_PROGRESS_EVENT', userData);
        };
        this.listenLoginInProgress = function (callback) {
            $rootScope.$on('LOGIN_IN_PROGRESS_EVENT', function (event, args) {
                callback(args);
            });
        };

        this.broadcastLoginSuccess = function (userData) {
            $rootScope.$broadcast('LOGIN_SUCCESS_EVENT', userData);
        };
        this.listenLoginSuccess = function (callback) {
            $rootScope.$on('LOGIN_SUCCESS_EVENT', function (event, args) {
                callback(args);
            });
        };

        this.broadcastLogout = function () {
            $rootScope.$broadcast('LOGOUT_EVENT');
        };
        this.listenLogout = function (callback) {
            $rootScope.$on('LOGOUT_EVENT', callback);
        };
    })
    .provider('sessionManagerService', function () {
        var currentUser;
        var isLoggedIn;

        function sessionManagerFactory(StrudelCore, sessionEventService, $log, $state) {

            currentUser = {
                userName: StrudelCore.session.currentUser().getUserName(),
                emailAddress: StrudelCore.session.currentUser().getEmailAddress()
            };
            isLoggedIn = StrudelCore.session.loginStatus() === "LOGGED_IN";

            sessionEventService.listenLoginInProgress(function(userData) {
                currentUser = userData;
                isLoggedIn = false;
                $log.info("Login in progress...");
            });
            sessionEventService.listenLoginSuccess(function(userData) {
                currentUser = userData;
                isLoggedIn = true;
                $state.go("home");
                $log.info("Login in complete. Logged in as:");
                $log.info(userData);
            });
            sessionEventService.listenLogout(function() {
                currentUser = {};
                isLoggedIn = false;
                $state.go("login");
                $log.info("Logout complete.");
            });

            return {
                isLoggedIn: function() { return isLoggedIn },
                currentUser: function() { return currentUser },
                doLogin: StrudelCore.session.triggerLogin,
                doLogout: StrudelCore.session.triggerLogout
            };
        }

        return {
            $get: sessionManagerFactory
        };

    });