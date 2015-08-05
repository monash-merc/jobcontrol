'use strict';

angular.module('jobcontrolApp', ['ngAnimate', 'ngResource', 'ngDialog', 'ui.router'])
    .run(function(StrudelCore, sessionEventService, jobcontrolEventService, $rootScope) {
        StrudelCore.session.registerStateChangeEvent(function(oldState, newState) {
            var userState = {
                userName: StrudelCore.session.currentUser().getUserName(),
                emailAddress: StrudelCore.session.currentUser().getEmailAddress()
            };
            switch (newState) {
                case "LOGGED_IN_AWAITING_CERTIFICATE":
                    sessionEventService.broadcastLoginInProgress(userState);
                    break;
                case "LOGGED_IN":
                    sessionEventService.broadcastLoginSuccess(userState);
                    break;
                case "NOT_LOGGED_IN":
                    sessionEventService.broadcastLogout();
                    break;
            }
            $rootScope.$digest();
        });

        StrudelCore.jobControl.registerStateChangeEvent(function(oldState, newState) {
            jobcontrolEventService.broadcastJobListChange(newState);
            $rootScope.$digest();
        });
    });