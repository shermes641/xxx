// Add error class to body on any error.  This is used for functional testing.
window.onerror = function() {
    document.getElementsByTagName('body')[0].className += ' javascript_error';
    return false;
}

// Initialize the mediation module
var mediationModule = angular.module( 'MediationModule', ['ngRoute', 'ngSanitize', 'appsControllers', 'distributorUsersControllers', 'eCPMFilter', 'waterfallFilters', 'requiredFieldFilters', 'ui.sortable', 'ui.bootstrap', 'escapeHtmlFilters']);

// Initialize controllers
var distributorUsersControllers = angular.module('distributorUsersControllers', ['ngRoute']);
var appsControllers = angular.module('appsControllers', ['ngRoute']);

// Routing
mediationModule.config(['$routeProvider', '$locationProvider', '$httpProvider', function($routeProvider, $locationProvider, $httpProvider) {
    $routeProvider.when('/distributors/:distributorID/apps/new', {
        controller: 'NewAppsController',
        templateUrl: 'assets/templates/apps/newAppModal.html'
    }).when('/distributors/:distributorID/analytics', {
        controller: 'AnalyticsController',
        templateUrl: 'assets/templates/analytics/analytics.html'
    }).when('/distributors/:distributorID/waterfalls/:waterfallID/edit', {
        controller: 'WaterfallController',
        templateUrl: 'assets/templates/waterfalls/edit.html'
    }).when('distributors/:distributorID/apps', {
        controller: 'NewAppsController'
    }).when('/signup', {
        controller: 'SignupController',
        templateUrl: 'assets/templates/distributor_users/signup.html'
    }).when('/login', {
        controller: 'LoginController',
        templateUrl: 'assets/templates/distributor_users/login.html'
    }).when('/distributor_users/forgot_password', {
        controller: 'ForgotPasswordController',
        templateUrl: 'assets/templates/distributor_users/forgot_password.html'
    }).when('/distributor_users/reset_password', {
        controller: 'ResetPasswordController',
        templateUrl: 'assets/templates/distributor_users/reset_password.html'
    });
    $locationProvider.html5Mode(true);
    $httpProvider.interceptors.push('httpErrorInterceptor');
}]);

// Factories
mediationModule.factory('fieldsFilled', [function(data, requiredFields) {
    // Check if all required fields are filled.
    return function(data, requiredFields) {
        for(var i=0; i < requiredFields.length; i++) {
            var field = data[requiredFields[i]];
            if(field === undefined || field === "" || field === null) {
                return false
            }
        }
        return true;
    };
}]);

// Service to share and update IDs between Angular controllers
mediationModule.service('sharedIDs', [function() {
    var appID = 0;
    var distributorID = 0;
    return {
        appID: function() {
            return appID;
        },
        setAppID: function(id) {
            appID = id;
        },
        distributorID: function() {
            return distributorID;
        },
        setDistributorID: function(id) {
            distributorID = id;
        }
    }
}]);

// Service for platform-specific IDs, names, and assets
mediationModule.service('platforms', [function() {
    var iosID = 1;
    var androidID = 2;
    var iOSPlatform = {id: iosID, name: "iOS", documentationPath: "IS/iOS+SDK"};
    var androidPlatform = {id: androidID, name: "Android", documentationPath: "AS/Android+SDK"};
    var logoSource = function(platformName, deactivated) {
        if(platformName) {
            var iconColor = deactivated ? 'grey' : 'white';
            return '/assets/images/' + platformName + '_icon_' + iconColor + '.png';
        } else {
            return '';
        }
    };
    var platforms = {
        ios: iOSPlatform,
        android: androidPlatform,
        logoSource: logoSource
    };
    platforms[iosID] = iOSPlatform;
    platforms[androidID] = androidPlatform;
    return {
        all: function() {
            return platforms;
        }
    }
}]);

// Register HTTP error interceptor as service
mediationModule.factory('httpErrorInterceptor', ['$q', 'flashMessage', function($q, flashMessage) {
    return {
        // handle response Error
        'responseError': function(rejection) {
            if(rejection.status === 503) {
                flashMessage.add({message: "We are currently down for maintenance.  Please try again later.", status: "error"});
            } else if(rejection.status === 0 || rejection.status.toString()[0] === "5") {
                flashMessage.add({message: "There was a problem with the request.  Please try again later.", status: "error"});
            }

            console.log("Intercepted HTTP response error with status code: " + rejection.status);
            return $q.reject(rejection);
        }
    };
}]);

/**
 *  All controllers use this factory to display flash messages in the UI.
 *
 *  When the first flash message is added to the messageQueue, it is removed and displayed
 *  Subsequent flash messages are placed on the messageQueue unless the message, status, and priority are the same as the currently displayed flash message
 *  After a delay, the next flash message is dequeued and displayed.
 *  The messageQueue is checked for duplicates of the currently displayed message, and they are removed.
 *  Flash messages with "HIGH" priority are placed at the front of the queue.
 *  Flash messages with "LOW" priority are placed at the tail of the queue. Flash messages with undefined priority are set to LOW priority.
 */
mediationModule.factory('flashMessage', ['$timeout', function($timeout) {
    var currentTimer;
    var currentMessage = '';
    var messageClass = '';
    var currentPriority;
    var messageQueue = [];

    // do not allow duplicate messages with the same status and priority on the messageQueue
    var removeDuplicates = function(msg, stat, priority){
        var len = messageQueue.length;
        // remove duplicate messages from the end to the start of the array
        for (var i = len-1; i > -1; i--) {
            if (messageQueue[i].message === msg &&
                messageQueue[i].status === stat &&
                messageQueue[i].priority === priority) {
                messageQueue.splice(i,1)
            }
        }
    };

    // Iterate through the message queue, showing each message for 5 seconds.
    var displayMessages = function() {
        if(messageQueue.length > 0) {
            var lastMessage = messageQueue.shift();
            currentMessage = lastMessage.message;
            currentPriority = lastMessage.priority;
            messageClass = lastMessage.status;
            if(messageQueue.length > 0)
                removeDuplicates(currentMessage, messageClass, currentPriority);

            if (typeof currentTimer != 'undefined')
                clearTimeout(currentTimer);

            currentTimer = $timeout(function() {
                currentMessage = '';
                messageClass = '';
                displayMessages();
            }, 5000);
        }
    };

    return {
        add: function(data) {
            if(typeof data === 'object' && typeof data.message === 'string') {
                //TODO we could have 3 priorities HIGH LOW undefined
                if (typeof data.priority === 'undefined')
                    data.priority = "LOW";
                // add message if the same message is not being displayed
                if (currentMessage !== data.message ||
                    messageClass !== data.status ||
                    currentPriority !== data.priority) {
                    if (data.priority === "HIGH") {
                        messageQueue.unshift(data);
                    } else{
                        messageQueue.push(data);
                    }
                }
            }
            if(currentMessage === '') {
                displayMessages();
            }
        },
        displayClass: function() {
            return messageClass;
        },
        text: function() {
            return currentMessage;
        },
        forceDisplay: function() {
            displayMessages();
            return messageQueue
        },
        queue: function() {
            return messageQueue
        }
    }
}]);

// Directives
var clearErrorOnChange = 'clearErrorOnChange';
mediationModule.directive(clearErrorOnChange, function() {
    return {
        require: 'ngModel',
        scope: false,
        link: function(scope, elm, attrs, ctrl) {
            scope.$watch(attrs.clearErrorOnChange, function() {
                var getForm = function(name) {
                    return name.split(".").reduce(function(formObj, formName) { return formObj[formName]; }, scope);
                };
                var form = getForm(attrs.formName || scope.formName);
                if(form.$submitted) {
                    scope.errors[attrs.name] = "";
                    scope.errors[attrs.name + "Class"] = "";
                }
            });
        }
    };
});

mediationModule.directive('modalDialog', function($rootScope) {
    return {
        restrict: 'E',
        scope: false,
        replace: true,
        transclude: true,
        link: function(scope, element, attrs) {
            scope.dialogStyle = {};
            if (attrs.width)
                scope.dialogStyle.width = attrs.width;
            if (attrs.height)
                scope.dialogStyle.height = attrs.height;
            scope.hideModal = function() {
                scope.errors = {};
                if(scope.showTestModeConfirmationModal) {
                    ga('send', 'event', 'testmode_toggle_close', 'click', 'waterfalls');
                    scope.waterfallData.waterfall.testMode = false;
                }
                scope.showModal(false);
                scope.showWaterfallAdProviderModal = false;
                scope.showEditAppModal = false;
                scope.data = {};
                scope.showNewAppModal = false;
                scope.showTestModeConfirmationModal = false;
                scope.showPauseConfirmationModal = false;
                $rootScope.bodyClass = "";
            };

            // Add body class to prevent scrolling when modal open
            scope.showModal = function(display) {
                scope.dialogStyle.overflowY = (scope.showEditAppModal || scope.showNewAppModal) ? "visible" : "auto";
                $rootScope.bodyClass = display ? "modal-active" : "";
                scope.modalShown = display;
            };

        },
        templateUrl: "assets/templates/apps/modal.html"
    };
});

mediationModule.directive('typeaheadFocus', function () {
        return {
            require: 'ngModel',
            link: function (scope, element, attr, ngModel) {
                //trigger the popup on 'click' because 'focus'
                //is also triggered after the item selection
                element.bind('focus', function () {
                    var viewValue = ngModel.$viewValue;

                    //restore to null value so that the typeahead can detect a change
                    if(ngModel.$viewValue == ' ') {
                        ngModel.$setViewValue(null);
                    }

                    //force trigger the popup
                    ngModel.$setViewValue(' ');

                    //set the actual value in case there was already a value in the input
                    ngModel.$setViewValue(viewValue || ' ');
                });

                //compare function that treats the empty space as a match
                scope.emptyOrMatch = function (actual, expected) {
                    if(expected == ' ') {
                        return true;
                    }
                    return actual.toString().toLowerCase().indexOf(expected.toString().toLowerCase()) > -1;
                };
            }
        };
    });

var invalidNumber = function(number) {
    if(typeof number === "string") {
        return number.match(/^[0-9]+?$/) === null
    } else {
        return false;
    }
};

mediationModule.directive('requiredInteger', function() {
    return {
        require: 'ngModel',
        link: function(scope, elm, attrs, ctrl) {
            ctrl.$validators.requiredInteger = function(modelValue) {
                return !(ctrl.$isEmpty(modelValue) || invalidNumber(modelValue) || parseInt(modelValue) < 1);
            };
        }
    };
});

mediationModule.directive('validateEcpm', function() {
    return {
        require: 'ngModel',
        link: function(scope, elm, attrs, ctrl) {
            ctrl.$validators.validateEcpm = function(modelValue) {
                var parsedCpm = parseFloat(modelValue);
                return !(isNaN(parsedCpm) || parsedCpm < 0 || (modelValue.match(/^[0-9]{0,}([\.][0-9]+)?$/) === null));
            };
        }
    };
});

var greaterThanDirectiveName = 'greaterThanOrEqualTo';
mediationModule.directive(greaterThanDirectiveName, function() {
    return {
        require: 'ngModel',
        link: function(scope, elm, attrs, ctrl) {
            var validate = function(viewValue) {
                var comparisonModel = attrs.greaterThanOrEqualTo;
                ctrl.$setValidity(greaterThanDirectiveName, (ctrl.$isEmpty(viewValue) || comparisonModel === "") ? true : parseInt(viewValue) >= parseInt(comparisonModel));
                return viewValue;
            };

            ctrl.$parsers.unshift(validate);
            ctrl.$formatters.push(validate);

            attrs.$observe(greaterThanDirectiveName, function() {
                return validate(ctrl.$viewValue);
            });
        }
    };
});

var lessThanDirectiveName = 'lessThanOrEqualTo';
mediationModule.directive(lessThanDirectiveName, function() {
    return {
        require: 'ngModel',
        link: function(scope, elm, attrs, ctrl) {
            var validate = function(viewValue) {
                var comparisonModel = attrs.lessThanOrEqualTo;
                ctrl.$setValidity(lessThanDirectiveName,  ctrl.$isEmpty(comparisonModel) ? true : parseInt(viewValue) <= parseInt(comparisonModel));
                return viewValue;
            };

            ctrl.$parsers.unshift(validate);
            ctrl.$formatters.push(validate);

            attrs.$observe(lessThanDirectiveName, function() {
                return validate(ctrl.$viewValue);
            });
        }
    };
});

var reportingRequired = 'reportingRequired';
mediationModule.directive(reportingRequired, function() {
    return {
        require: 'ngModel',
        link: function(scope, elm, attrs, ctrl) {
            var validate = function(viewValue) {
                ctrl.$setValidity(reportingRequired, attrs.reportingRequired === "true" ? !ctrl.$isEmpty(viewValue) : true);
                return viewValue;
            };

            ctrl.$parsers.unshift(validate);
            ctrl.$formatters.push(validate);

            attrs.$observe(reportingRequired, function() {
                return validate(ctrl.$viewValue);
            });
        }
    };
});

mediationModule.directive('rewardMaxValidator', function() {
    return {
        require: 'ngModel',
        link: function(scope, elm, attrs, ctrl) {
            ctrl.$validators.rewardMaxValidator = function(modelValue, viewValue) {
                return !(!ctrl.$isEmpty(modelValue) && invalidNumber(modelValue));
            };
        }
    };
});

var callbackValidator = 'callbackValidator';

mediationModule.directive(callbackValidator, function() {
    return {
        require: 'ngModel',
        link: function(scope, elm, attrs, ctrl) {
            var validate = function(viewValue) {
                var serverToServerEnabled = attrs.callbackValidator === "true";
                if(serverToServerEnabled || !ctrl.$isEmpty(ctrl.$viewValue)) {
                    ctrl.$setValidity(callbackValidator, (/(http|https):\/\//).test(ctrl.$viewValue));
                } else {
                    ctrl.$setValidity(callbackValidator, true);
                }
                return viewValue;
            };

            ctrl.$parsers.unshift(validate);
            ctrl.$formatters.push(validate);

            attrs.$observe(callbackValidator, function(){
                return validate();
            });
        }
    };
});

var passwordConfirmation = 'passwordConfirmation';

mediationModule.directive(passwordConfirmation, function() {
    return {
        require: 'ngModel',
        link: function(scope, elm, attrs, ctrl) {
            var validate = function(confirmation) {
                var password = attrs.passwordConfirmation;
                if(password !== undefined && password.length > 7) {
                    ctrl.$setValidity(passwordConfirmation, confirmation === password);
                } else {
                    ctrl.$setValidity(passwordConfirmation, true);
                }
                return confirmation;
            };

            ctrl.$parsers.unshift(validate);
            ctrl.$formatters.push(validate);

            attrs.$observe(passwordConfirmation, function(password) {
                return validate(ctrl.$viewValue);
            });
        }
    };
});

var validateRequiredParamFormat = 'validateRequiredParamFormat';

mediationModule.directive(validateRequiredParamFormat, function() {
    return {
        require: 'ngModel',
        link: function(scope, elm, attrs, ctrl) {
            var validate = function() {
                if(attrs.requiredParamDataType === "Array") {
                    ctrl.$setValidity(validateRequiredParamFormat, attrs.validateRequiredParamFormat.split(",").filter(function(zoneID) { return zoneID.match(/^(^$|\s+)$/); }).length === 0);
                } else {
                    ctrl.$setValidity(validateRequiredParamFormat, true);
                }
            };

            attrs.$observe(validateRequiredParamFormat, function(){
                return validate(ctrl.$viewValue);
            });
        }
    };
});

// Filters
angular.module('eCPMFilter', []).filter('monetaryFormat', function() {
    return function(value) {
        if(value === null) {
            return "";
        } else {
            var formatted = Math.floor(100 * value) / 100;
            return parseFloat(formatted).toFixed(2);
        }
    };
});

angular.module('waterfallFilters', []).filter('waterfallStatus', function() {
    return function(status) {
        return status ? "Active" : "Inactive";
    };
});

angular.module('requiredFieldFilters', []).filter('conditionalRequiredField', function() {
    return function(fieldName, condition) {
        return condition ? "*" + fieldName : fieldName;
    };
});

angular.module('escapeHtmlFilters', []).filter('escapeHtml', function($sce) {
    return function(value) {
        return $sce.trustAsHtml(value);
    }
});
