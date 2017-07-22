<#include "header.ftl">
<#assign restartInfo = server.getRestartingInfo()>
<h1>Advanced Settings
    <small><a href="/server?server=${server.ID?js_string}">${server.ID?js_string}</a> <span id="online"></span></small>
</h1>
<div class="row">
    <div class="col-sm-6">
        <div class="panel panel-info">
            <div class="panel-heading">
                <h3 class="panel-title" style="text-align: center;">Restart info</h3>
            </div>
            <div class="panel-body" style="text-align: center;">
                <p>Last Auto Restart: ${restartInfo.getLastRestart("YYYY-MM-dd hh:mm:ss")}</p>
                <p>Next Auto Restart: ${restartInfo.getNextRestart("YYYY-MM-dd hh:mm:ss")}</p>

                <div class="form-group">
                    <label>
                        <input id="RestartingInfo_autoStart" type="checkbox"> Autostart
                    </label>
                    <span for="RestartingInfo_autoStart" class="help-block">Start the server when the backend starts.</span>
                </div>
                <div class="form-group">
                    <label>
                        <input id="RestartingInfo_enableRestartSchedule" type="checkbox"> Use a reboot schedule
                    </label>
                    <span for="RestartingInfo_enableRestartSchedule" class="help-block">Enable daily restarts.</span>
                </div>
                <div class="form-group">
                    <label for="RestartingInfo_restartScheduleHours">Reboot schedule time</label>

                    <div class="input-group col-sm-6 col-sm-offset-3">
                        <input id="RestartingInfo_restartScheduleHours" class="form-control" aria-describedby="helpBlock" type="number" min="0" max="23" placeholder="00">

                        <div class="input-group-addon">h</div>
                        <input id="RestartingInfo_restartScheduleMinutes" class="form-control" aria-describedby="helpBlock" type="number" min="0" max="59" placeholder="00">
                    </div>
                    <span for="RestartingInfo_restartScheduleHours" class="help-block">This is server time!</span>
                </div>
                <div class="form-group">
                    <label for="RestartingInfo_restartScheduleMessage">Reboot schedule time</label>

                    <div class="input-group">
                        <input id="RestartingInfo_restartScheduleMessage" class="form-control" aria-describedby="helpBlock" placeholder="00:00" maxlength="100">

                        <div class="input-group-addon">%time = time left in min</div>
                    </div>
                    <span for="RestartingInfo_restartScheduleMessage" class="help-block">This message gets send at: T- 15, 10, 5, 4, 3, 2, 1, 0 min.</span>
                </div>
            </div>
            <div class="panel-footer">
                <button id="submit" type="submit" class="btn btn-primary btn-block" onclick="send('RestartingInfo');">Save!</button>
            </div>
        </div>
    </div>
    <div class="col-sm-6">
        <div class="panel panel-info">
            <div class="panel-heading">
                <h3 class="panel-title" style="text-align: center;">JVM options</h3>
            </div>
            <div class="panel-body" style="text-align: center;">
                <div class="form-group">
                    <label for="JvmData_jarName">Server jar name</label>
                    <select id="JvmData_jarName" class="form-control" aria-describedby="helpBlock">
                    <#list server.getPossibleJarnames() as jarName>
                        <option value="${jarName?json_string}">${jarName?html}</option>
                    </#list>
                    </select>
                </div>
                <div class="form-group">
                    <label for="JvmData_ramMin">Server RAM</label>

                    <div class="input-group">
                        <div class="input-group-addon">Min:</div>
                        <input id="JvmData_ramMin" class="form-control" aria-describedby="helpBlock" type="number" min="512" step="512">

                        <div class="input-group-addon">MB</div>
                    </div>
                </div>
                <div class="form-group">
                    <div class="input-group">
                        <div class="input-group-addon">Max:</div>
                        <input id="JvmData_ramMax" class="form-control" aria-describedby="helpBlock" type="number" min="512" step="512">

                        <div class="input-group-addon">MB</div>
                    </div>
                </div>
                <div class="form-group">
                    <label for="JvmData_extraJavaParameters">Java Parameters</label>
                    <input id="JvmData_extraJavaParameters" class="form-control" aria-describedby="helpBlock" type="text">
                </div>
                <div class="form-group">
                    <label for="JvmData_extraMCParameters">MC Parameters</label>
                    <input id="JvmData_extraMCParameters" class="form-control" aria-describedby="helpBlock" type="text">
                </div>
            </div>
            <div class="panel-footer">
                <button id="submit" type="submit" class="btn btn-primary btn-block" onclick="send('JvmData');">Save!</button>
            </div>
        </div>
    </div>
</div>
<script>
    var firstRun = true;
    var allkeys = {};
    function updateInfo(data)
    {
        for (key1 in data)
        {
            if (!data.hasOwnProperty(key1)) continue;
            for (key2 in data[key1])
            {
                if (!data[key1].hasOwnProperty(key2)) continue;
                if (firstRun)
                {
                    if (typeof allkeys[key1] === "undefined") allkeys[key1] = [];
                    allkeys[key1].push(key2);
                }
                var dom = get(key1 + "_" + key2);
                if (dom != null)
                {
                    if (dom.type === "checkbox")
                    {
                        dom.checked = data[key1][key2];
                    }
                    else
                    {
                        dom.value = data[key1][key2];
                    }
                }
            }
        }
        firstRun = false;
    }

    var websocket = new WebSocket(wsurl("advancedsettings/${server.ID?js_string}"));
    websocket.onmessage = function (evt)
    {
        var temp = JSON.parse(evt.data);
        if (temp.status === "ok")
        {
            updateInfo(temp.data);
        }
        else
        {
            addAlert(temp.message, true);
        }
    };
    websocket.onerror = function (evt)
    {
        addAlert("The websocket errored. Refresh the page!")
    };
    websocket.onclose = function (evt)
    {
        addAlert("The websocket closed. Refresh the page!")
    };

    function send(key1)
    {
        var data = {};
        data[key1] = {};
        for (key2 in allkeys[key1])
        {
            var name = key1 + "_" + allkeys[key1][key2];
            var dom = get(name);
            if (dom == null) continue;
            if (dom.type === "checkbox")
            {
                data[key1][allkeys[key1][key2]] = dom.checked;
            }
            else
            {
                data[key1][allkeys[key1][key2]] = dom.value;
            }
        }
        websocket.send(JSON.stringify(data));
    }
</script>
<#include "footer.ftl">
