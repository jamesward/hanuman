$(function() {
    var simulationId;
    var debug = false;
    var running = false;
    var previousMatchedPortion = "";

    function createSimulation() {
        $.ajax("/newSimulation", {
            contentType: "application/json",
            success: onCreateSimulation
        });
    }

    function onCreateSimulation(data) {
        if (data.id) {
            simulationId = data.id;
            $("#newSimulationButton").hide();
            //$("#results").html()
            $("#stopSimulationButton").click(stopSimulation);
            $("#stopSimulationButton").show();
            $("#results").show();
            runSimulation();
        }
    }

    function runSimulation() {
        running = true;
        if (debug==true)
            $("#debug").show()
        $("#debug").append("runSimulation(): About to invoke run<br/>\n");
        $.ajax("/run/" + simulationId, {
            contentType: "application/json",
            success: onRunSuccess,
            error: onError
        });
    }

    function onError(jqXHR, textStatus, errorThrown) {
        $("#debug").append("onError: " + textStatus + " "  + errorThrown +"<br/>\n")
    }

    function onRunSuccess(data) {
        if (debug==true)
            $("#debug").append("onRunSuccess(): About to check for data.result<br/>\n");
        if (data.result) {
            //$("#results").append("<li>" + data.result + "</li>");
            onGetSimulationStatus(data)
            window.setInterval(getSimulationStatus, 1500);
        }
    }

    function getSimulationStatus() {
        $("#debug").append("getSimulationStatus(): About to invoke status<br/>\n");
        $.ajax("/status/" + simulationId, {
            contentType: "application/json",
            success: onGetSimulationStatus,
            error: onError
        });
    }

    // data will be similar to:
    // {"result":{"complete":false,"id":"a2f57249-d739-49c5-b54b-596623013de7","length":0,"formattedElapsedTime":"00:00:00","formattedTimeStarted":"02:10:40 PM","maxTicks":100,"percentComplete":0,"tick":0,"monkeys":10,"matchedPortion":"","version":"0.2"}}
    function onGetSimulationStatus(data) {
        if (data.result) {
            $("#version")        .html(data.result.version);
            $("#started")        .html(data.result.formattedTimeStarted);
            $("#elapsed")        .html(data.result.formattedElapsedTime);
            $("#tick")           .html(data.result.tick);
            $("#maxTicks")       .html(data.result.maxTicks);
            $("#percentComplete").html(data.result.percentComplete);
            $("#monkeys")        .html(data.result.monkeys);
            var newMatchedPortion = data.result.matchedPortion.substring(previousMatchedPortion.length-1, data.result.matchedPortion.length-1)
            $("#matchedPortion") .html(previousMatchedPortion + '<span class="newMatchedPortion">' + newMatchedPortion + '</span>');
            previousMatchedPortion = data.result.matchedPortion;
            if (data.result.complete) {
                $("#stopSimulationButton").hide("slow");
                $("#newSimulationButton").show("slow");
                // do other things to show user simulation is done
                // stop querying server
            }
        }
    }

    function stopSimulation() {
        running = false;
        $("#debug").html();
        $("#debug").hide();
        $("#results").hide();
        $("#stopSimulationButton").hide();
        $("#newSimulationButton").show();
        $.ajax("/stop", {
            contentType: "application/json"
        });
    }

    function toggleDebug(){
        debug = !debug;
        if (debug==true && running==true)
           $("#debug").show("slow");
        else
           $("#debug").hide("slow");
    }

    $("body")
        .append('<h1 style="font-family: arial">Hanuman <span id="version"></span></h1>\n')
        .append('<p><a href="https://github.com/mslinn/hanuman" target="src">Source on GitHub</a><br/>' +
                '<a href="http://www.slideshare.net/mslinn/hanuman-10278606" target="vid">Presentation on SlideShare</a></p>\n')
        .append('<button id="newSimulationButton">Start a new simulation</button>\n')
        .append('<div id="results" class="results"></div>\n')
        .append('<div id="debug" class="debug"></div>\n')
        .append('<a href="http://micronauticsresearch.com" target="mr"><img src="http://micronauticsresearch.com/images/logo-300x91.png" style="position: absolute; bottom: 5; left: 5" /></a>\n')
        .append('<div class="debugCheckbox">Debug <input type="checkbox" id="debugCheckbox" />\n</div>\n')
        .append('<a href="http://heroku.com" target="h"><img src="https://nav.heroku.com/images/logos/logo.png" style="position: absolute; bottom: 5; right: 5" /></a>\n');
    $("#debug").hide()
    $("#results").append('<span class=label>Simulation ID</span> <tt>' + simulationId + '</tt> &nbsp;&nbsp;')
                 .append('<button id="stopSimulationButton">Stop simulation</button>\n')
                 .append('<br/><br/>\n')
                 .append('<span class=label>Started at</span> <span id="started"></span>; <span class=label>elapsed time</span> <span id="elapsed">00:00:00</span><br/><br/>\n')
                 .append('<span class=label>Tick</span> <span id="tick">0</span> of <span id="maxTicks"></span>; <span id="percentComplete"></span> % complete <br/><br/>\n')
                 .append('<span id="match">0</span> characters matched so far:<br/>\n')
                 .append('<div id="matchedPortion" class="matchedPortion"></div>\n').hide();
    $("#newSimulationButton").click(createSimulation);
    $("#debugCheckbox").click(toggleDebug);
})