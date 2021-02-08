#!/usr/bin/env node

var TARGET_ATTRIBUTE = 'org.apache.cordova.firebase.InfodiggMessagingService';
var TARGET_VALUE = 'org.apache.cordova.firebase.FirebasePluginMessagingService';

module.exports = function(context) {
	var fs = require('fs');
	var path = require("path");
	
	var platformRoot = path.join(context.opts.projectRoot, 'platforms/android');
	var manifestFile = path.join(platformRoot, 'app/src/main/AndroidManifest.xml');

	if (fs.existsSync(manifestFile)) {
		fs.readFile(manifestFile, 'utf8', function (err, data) {
			if (err) {
				throw new Error('Unable to find AndroidManifest.xml: ' + err);
			}
			if(data.indexOf(TARGET_ATTRIBUTE) > 0) {
				var result = data.replace(TARGET_ATTRIBUTE, TARGET_VALUE);
				fs.writeFile(manifestFile, result, 'utf8', function (err) {
					if (err) throw new Error('Unable to write AndroidManifest.xml: ' + err);
				});
			}
		});
	}
};