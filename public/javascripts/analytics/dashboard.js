!function(a,b){if(void 0===b[a]){b["_"+a]={},b[a]=function(c){b["_"+a].clients=b["_"+a].clients||{},b["_"+a].clients[c.projectId]=this,this._config=c},b[a].ready=function(c){b["_"+a].ready=b["_"+a].ready||[],b["_"+a].ready.push(c)};for(var c=["addEvent","setGlobalProperties","trackExternalLink","on"],d=0;d<c.length;d++){var e=c[d],f=function(a){return function(){return this["_"+a]=this["_"+a]||[],this["_"+a].push(arguments),this}};b[a].prototype[e]=f(e)}var g=document.createElement("script");g.type="text/javascript",g.async=!0,g.src="https://d26b395fwzu5fz.cloudfront.net/3.0.7/keen.min.js";var h=document.getElementsByTagName("script")[0];h.parentNode.insertBefore(g,h)}}("Keen",this);

var client = new Keen({
    projectId: "53f75d42709a3952e3000002",
    readKey: "38e91b786e4c8150f22eac2368b038bc50d7e2a6904e97578a32e11d08a89b1ec1192272df9d9b7ca2586d5852e059f5604c702ded6d914ba68f14e8049d6023b076555e23500a8baf660c503b038a0a3fc9050872441938525c888a65cb49b85186e1b060fa5ceb8256351ef22c0902"
});

function isValidDate( date ) {
    if ( isNaN( date.getTime() ) ) {  // d.valueOf() could also work
        return false;
    }
    return true;
}

function updateCharts( event ) {
    var start_date = $('#start_date').datepicker('getDate');
    var end_date = $('#end_date').datepicker('getDate');
    // Return if one or both of the dates are invalid
    if (!isValidDate(start_date) || !isValidDate(end_date)) {
        return;
    }

    // Return if start date after end date
    if (end_date.getTime() === start_date.getTime()) {
        return;
    }

    var start_date_iso = start_date.toISOString();
    var end_date_iso = end_date.toISOString();

    Keen.ready( function() {
        // Inventory Request count, metric
        var inventory_request = new Keen.Query( "count", {
            eventCollection: "Inventory Request",
            filters: [ {
                property_name: "app",
                operator: "eq",
                property_value: 1
            } ]
        } );
        client.draw( inventory_request, document.getElementById( "inventory_requests" ), {
            chartType: "metric",
            title: "Requests",
            colors: [ "#4285f4" ],
            width: $( "#inventory_requests" ).width()
        });

        // Unique Users count, metric
        var unique_users = new Keen.Query( "count", {
            eventCollection: "Inventory Request",
            filters: [ {
                property_name: "app",
                operator: "eq",
                property_value: 1
            } ]
        } );
        client.draw( unique_users, document.getElementById( "unique_users" ), {
            chartType: "metric",
            title: "Unique Users",
            colors: [ "#4285f4" ],
            width: $( "#unique_users" ).width()
        });

        // Fill rate, metric
        var fill_rate = new Keen.Query( "count", {
            eventCollection: "Inventory Request",
            filters: [ {
                property_name: "app",
                operator: "eq",
                property_value: 1
            } ]
        } );
        client.draw( fill_rate, document.getElementById( "fill_rate" ), {
            chartType: "metric",
            title: "Fill Rate",
            colors: [ "#4285f4" ],
            width: $( "#fill_rate" ).width()
        });

        // Estimated Revenue, table
        var estimated_revenue = new Keen.Query( "count", {
            eventCollection: "Inventory Request",
            filters: [ {
                property_name: "app",
                operator: "eq",
                property_value: 1
            } ]
        } );
        client.draw( estimated_revenue, document.getElementById( "estimated_revenue" ), {
            chartType: "metric",
            title: "Estimated Revenue",
            colors: [ "#4285f4" ],
            width: $( "#estimated_revenue" ).width()
        });

        // Estimated Revenue Chart
        var pageviews_timeline = new Keen.Query("count", {
            eventCollection: "pageviews",
            interval: "hourly",
            groupBy: "user.device_info.browser.family",
            timeframe: {
                start: start_date_iso,
                end: end_date_iso
            }
        });
        client.draw(pageviews_timeline, document.getElementById("estimated_revenue_chart"), {
            chartType: "areachart",
            title: false,
            height: 250,
            width: "auto",
            chartOptions: {
                chartArea: {
                    height: "85%",
                    left: "5%",
                    top: "5%",
                    width: "80%"
                },
                isStacked: true
            }
        });
    });
};


$('.input-daterange').datepicker({
    orientation: "top left"
}).on("changeDate", updateCharts);

$('#start_date').datepicker('setDate', '-1m');
$('#end_date').datepicker('setDate', '0');