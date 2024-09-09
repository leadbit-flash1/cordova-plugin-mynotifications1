var exec = require('cordova/exec');

var LocalNotificationsPlugin = {
    hasPermission: function(successCallback) {
        exec(successCallback, null, "_MYNOTIFICATIONS1", "hasPermission", []);
    },
    requestPermission: function(successCallback) {
        exec(successCallback, null, "_MYNOTIFICATIONS1", "requestPermission", []);
    },
    schedule: function(options) {
        exec(null, null, "_MYNOTIFICATIONS1", "schedule", [options]);
    },
	clearAll: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, "_MYNOTIFICATIONS1", "clearAll", []);
    }
};

module.exports = LocalNotificationsPlugin;