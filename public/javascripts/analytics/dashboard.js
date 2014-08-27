!function(a,b){if(void 0===b[a]){b["_"+a]={},b[a]=function(c){b["_"+a].clients=b["_"+a].clients||{},b["_"+a].clients[c.projectId]=this,this._config=c},b[a].ready=function(c){b["_"+a].ready=b["_"+a].ready||[],b["_"+a].ready.push(c)};for(var c=["addEvent","setGlobalProperties","trackExternalLink","on"],d=0;d<c.length;d++){var e=c[d],f=function(a){return function(){return this["_"+a]=this["_"+a]||[],this["_"+a].push(arguments),this}};b[a].prototype[e]=f(e)}var g=document.createElement("script");g.type="text/javascript",g.async=!0,g.src="https://d26b395fwzu5fz.cloudfront.net/3.0.7/keen.min.js";var h=document.getElementsByTagName("script")[0];h.parentNode.insertBefore(g,h)}}("Keen",this);

var client = new Keen({
    projectId: "53f75d42709a3952e3000002",
    readKey: "38e91b786e4c8150f22eac2368b038bc50d7e2a6904e97578a32e11d08a89b1ec1192272df9d9b7ca2586d5852e059f5604c702ded6d914ba68f14e8049d6023b076555e23500a8baf660c503b038a0a3fc9050872441938525c888a65cb49b85186e1b060fa5ceb8256351ef22c0902"
});

var client_demo = new Keen({
    projectId: "5368fa5436bf5a5623000000",
    readKey: "3f324dcb5636316d6865ab0ebbbbc725224c7f8f3e8899c7733439965d6d4a2c7f13bf7765458790bd50ec76b4361687f51cf626314585dc246bb51aeb455c0a1dd6ce77a993d9c953c5fc554d1d3530ca5d17bdc6d1333ef3d8146a990c79435bb2c7d936f259a22647a75407921056"
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

    Keen.ready(function(){

        // ----------------------------------------
        // Pageviews Area Chart
        // ----------------------------------------
        var pageviews_timeline = new Keen.Query("count", {
            eventCollection: "pageviews",
            interval: "hourly",
            groupBy: "user.device_info.browser.family",
            timeframe: {
                start: "2014-05-04T00:00:00.000Z",
                end: "2014-05-05T00:00:00.000Z"
            }
        });
        client_demo.draw(pageviews_timeline, document.getElementById("chart-01"), {
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


        // ----------------------------------------
        // Pageviews Pie Chart
        // ----------------------------------------
        var pageviews_static = new Keen.Query("count", {
            eventCollection: "pageviews",
            groupBy: "user.device_info.browser.family",
            timeframe: {
                start: "2014-05-04T00:00:00.000Z",
                end: "2014-05-05T00:00:00.000Z"
            }
        });
        client_demo.draw(pageviews_static, document.getElementById("chart-02"), {
            chartType: "piechart",
            title: false,
            height: 250,
            width: "auto",
            chartOptions: {
                chartArea: {
                    height: "85%",
                    left: "5%",
                    top: "5%",
                    width: "100%"
                }
            }
        });


        // ----------------------------------------
        // Impressions timeline
        // ----------------------------------------
        var impressions_timeline = new Keen.Query("count", {
            eventCollection: "impressions",
            groupBy: "ad.advertiser",
            interval: "hourly",
            timeframe: {
                start: "2014-05-04T00:00:00.000Z",
                end: "2014-05-05T00:00:00.000Z"
            }
        });
        client_demo.draw(impressions_timeline, document.getElementById("chart-03"), {
            chartType: "columnchart",
            title: false,
            height: 250,
            width: "auto",
            chartOptions: {
                chartArea: {
                    height: "75%",
                    left: "10%",
                    top: "5%",
                    width: "60%"
                },
                bar: {
                    groupWidth: "85%"
                },
                isStacked: true
            }
        });


        // ----------------------------------------
        // Impressions timeline (device)
        // ----------------------------------------
        var impressions_timeline_by_device = new Keen.Query("count", {
            eventCollection: "impressions",
            groupBy: "user.device_info.device.family",
            interval: "hourly",
            timeframe: {
                start: "2014-05-04T00:00:00.000Z",
                end: "2014-05-05T00:00:00.000Z"
            }
        });
        client_demo.draw(impressions_timeline_by_device, document.getElementById("chart-04"), {
            chartType: "columnchart",
            title: false,
            height: 250,
            width: "auto",
            chartOptions: {
                chartArea: {
                    height: "75%",
                    left: "10%",
                    top: "5%",
                    width: "60%"
                },
                bar: {
                    groupWidth: "85%"
                },
                isStacked: true
            }
        });


        // ----------------------------------------
        // Impressions timeline (country)
        // ----------------------------------------
        var impressions_timeline_by_country = new Keen.Query("count", {
            eventCollection: "impressions",
            groupBy: "user.geo_info.country",
            interval: "hourly",
            timeframe: {
                start: "2014-05-04T00:00:00.000Z",
                end: "2014-05-05T00:00:00.000Z"
            }
        });
        client_demo.draw(impressions_timeline_by_country, document.getElementById("chart-05"), {
            chartType: "columnchart",
            title: false,
            height: 250,
            width: "auto",
            chartOptions: {
                chartArea: {
                    height: "75%",
                    left: "10%",
                    top: "5%",
                    width: "60%"
                },
                bar: {
                    groupWidth: "85%"
                },
                isStacked: true
            }
        });

        var inventory_requests_per_day = new Keen.Query("count", {
            eventCollection: "Inventory Request",
            filters: [{"property_name":"distributor_id","operator":"eq","property_value":1}],
            groupBy: "app",
            timeframe: {
                start: start_date_iso,
                end: end_date_iso
            }
        });

        client.draw(inventory_requests_per_day, document.getElementById("table_by_day"), {
            chartType: "table",
            title: "Per Day",
            width: $("#table_by_day").width(),
            cssClassNames: {
                headerRow: "dashboard_table_header_row"
            }
        });

        var query = new Keen.Query("count", {
            eventCollection: "Inventory Request",
            filters: [{"property_name":"app","operator":"eq","property_value":1},
                {"property_name":"distributor_id","operator":"eq","property_value":1}]
        });
        client.draw(query, document.getElementById("inventory_requests"), {
            chartType: "metric",
            title: "Requests",
            colors: ["#4285f4"],
            width: $("#inventory_requests").width()
        });
        client.draw(query, document.getElementById("unique_users"), {
            chartType: "metric",
            title: "Unique Users",
            colors: ["#4285f4"],
            width: $("#unique_users").width()
        });
        client.draw(query, document.getElementById("fill_rate"), {
            chartType: "metric",
            title: "Fill Rate",
            colors: ["#4285f4"],
            width: $("#fill_rate").width()
        });
        client.draw(query, document.getElementById("estimated_revenue"), {
            chartType: "metric",
            title: "Estimated Revenue",
            colors: ["#4285f4"],
            width: $("#estimated_revenue").width()
        });

    });
};


$('.input-daterange').datepicker({
    orientation: "top left"
}).on("changeDate", updateCharts);

$('#start_date').datepicker('setDate', '-1m');
$('#end_date').datepicker('setDate', '0');