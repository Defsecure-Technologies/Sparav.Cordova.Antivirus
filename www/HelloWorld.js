var exec = require('cordova/exec');

exports.nativeToast = function (arg0, success, error) {
    exec(success, error, 'HelloWorld', 'nativeToast', [arg0]);
};

exports.getProcessing = function (arg0, success, error) {
    exec(success, error, 'HelloWorld', 'getProcessing', [arg0]);
};

exports.batterySaver = function(arg0, success, error) {
    exec(success, error, 'HelloWorld', 'batterySaver', [arg0]);
}

exports.vpnConnect = function(arg0, success, error) {
    exec(success, error, 'HelloWorld', 'startVpn', [arg0]);
}

exports.disconnectVpn = function(arg0, success, error) {
    exec(success, error, 'HelloWorld', 'disconnectVpn', [arg0]);
}
