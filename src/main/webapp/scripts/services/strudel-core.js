'use strict';

angular.module('jobcontrolApp')
    .factory('StrudelCore', function() {
        // This is exported by GWT and wrapped as an angular service
        return __strudel;
    });