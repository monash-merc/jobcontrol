'use strict';

angular.module('jobcontrolApp')
    .service('jobcontrolEventService', function ($rootScope) {
        this.broadcastJobListChange = function (newJobList) {
            $rootScope.$broadcast('JOB_LIST_UPDATE', newJobList);
        };
        this.listenJobListChange = function (callback) {
            $rootScope.$on('JOB_LIST_UPDATE', function (event, args) {
                callback(args);
            });
        };
    });