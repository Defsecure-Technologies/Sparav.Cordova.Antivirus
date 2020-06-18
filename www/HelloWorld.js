var exec = require('cordova/exec');

exports.nativeToast = function (arg0, success, error) {
    exec(success, error, 'HelloWorld', 'nativeToast', [arg0]);
};

exports.getProcessing = function (arg0, success, error) {
    exec(success, error, 'HelloWorld', 'getProcessing', [arg0]);
};


