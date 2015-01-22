"use strict";

(function(){
    var flash = $("#flash");
    var header = $("#username_header");

    $(document).ready(function() {
        flash.click(function(event) {
            flash.hide();
        });
    });
}());
