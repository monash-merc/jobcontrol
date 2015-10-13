'use strict';

angular.module('strudelWeb.version', [
  'strudelWeb.version.interpolate-filter',
  'strudelWeb.version.version-directive'
])

.value('version', '0.1');
