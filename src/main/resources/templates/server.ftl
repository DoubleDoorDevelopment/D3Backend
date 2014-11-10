<#include "header.ftl">
<#assign isCoOwner = server.isCoOwner(user) || user.isAdmin() >
<#assign isOwner = server.ownerObject == user || user.isAdmin() >
<h1>${server.ID}
    <small> ${server.getDisplayAddress()}   <span id="online"></span></small>
</h1>
<div class="btn-group">
    <button type="button" id="startServerBtn" class="btn btn-success">Start</button>
    <button type="button" class="btn btn-info" onclick="openPopup('/serverconsole?server=${server.ID}')">Console</button>
    <button type="button" id="stopServerBtn" class="btn btn-warning">Stop</button>
    <button type="button" id="killServerBtn" class="btn btn-danger">Kill</button>
</div>
<div class="row">
    <div class="col-sm-6">
        <div class="panel panel-info">
            <div class="panel-heading">
                <h3 class="panel-title" style="text-align: center;">Server information</h3>
            </div>
            <div class="panel-body" style="text-align: center;">
                <p>
                    Server owner is <span id="serverOwner"></span>.<br>
                    Server port status: <span id="serverPortAvailable"></span><br>
                    RCon port status: <span id="rconPortAvailable"></span><br>
                    <hr>
                    Diskspace in use:<br>
                    by server: <span id="diskspace_server"></span>MB<br>
                    by backups: <span id="diskspace_backup"></span>MB<br>
                    total: <span id="diskspace_total"></span>MB
                </p>
                <table class="table table-hover table-condensed">
                    <thead>
                    <tr>
                        <th style="text-align: right;width: 33%">Property</th>
                        <th style="text-align: left;">Value</th>
                    </tr>
                    </thead>
                    <tbody>
                    <#list ["ID", "onlinePlayers", "slots", "motd", "gameMode", "mapName", "playerList", "plugins", "version", "gameID"] as key>
                        <tr>
                            <td style="text-align: right;">${key}</td>
                            <td style="text-align: left;" id="${key}"></td>
                        </tr>
                    </#list>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
    <div class="col-sm-6">
        <div class="panel panel-danger">
            <div class="panel-heading">
                <h3 class="panel-title" style="text-align: center;">Danger zone&#x2122;</h3>
            </div>
            <div class="panel-body" style="text-align: center;">
                <div class="btn-group">
                    <a type="button" href='/worldmanager?server=${server.ID}' class="btn btn-info">World Manager</a>
                    <a type="button" href='/filemanager?server=${server.ID}' class="btn btn-info">File Manager</a>
                </div>
                <hr>
                <a type="button" id="killServerBtn" class="btn btn-default" href="/serverproperties?server=${server.ID}">Modify server properties</a>
                <hr>
                <div class="btn-group">
                    <button type="button" <#if isOwner>onclick="var name = prompt('Username of the future owner?'); if (name != null && confirm('Are you sure?')) {callOnThisServer('setOwner', name);}"<#else>disabled</#if> class="btn btn-danger">Change owner</button>
                    <button type="button" <#if isOwner && !server.online>onclick="if (confirm('Are you sure?\nThis will remove all files related to this server!')) {callOnThisServer('delete'); window.location='/servers'}"<#else>disabled</#if> class="btn btn-danger">Delete server</button>
                </div>
                <hr>
                <h4>Co-owners <#if isOwner><small style="cursor: pointer;" onclick="var name = prompt('Username of the future co-owner?');if (name != null && name !== '') {callOnThisServer('addCoowner|' + name)}"><i class="fa fa-plus"></i></small></#if></h4>
                <ul class="list-unstyled">
                    <#list server.getCoOwners() as name>
                        <li>${name}<#if isOwner><i style="cursor: pointer;" onclick="callOnThisServer('removeCoowner|${name}')" class="fa fa-times"></i></#if></li>
                    </#list>
                </ul>
                <p class="text-muted">Usernames on the backend. Can do everything except modify co-owners, change owner and delete the server.</p>
                <hr>
                <h4>Admins <#if isCoOwner><small style="cursor: pointer;" onclick="var name = prompt('Username of the future admin?');if (name != null && name !== '') {callOnThisServer('addAdmin|' + name)}"><i class="fa fa-plus"></i></small></#if></h4>
                <ul class="list-unstyled">
                <#list server.getAdmins() as name>
                    <li>${name}<#if isOwner><i style="cursor: pointer;" onclick="callOnThisServer('removeAdmin|${name}')" class="fa fa-times"></i></#if></li>
                </#list>
                </ul>
                <p class="text-muted">Usernames on the backend. One per line. Can start, stop & use console.</p>
            </div>
        </div>
    </div>
</div>
<div class="col-sm-12">
    <div class="form-group">
        <label for="mcVersionSelector">MC jar version</label>
        <select id="mcVersionSelector" class="form-control">
        <#list Helper.getAllMCVersions() as version>
            <option>${version}</option></#list>
        </select>

        <p class="help-block">To see any progress or errors, open the console.</p>
    </div>
    <button type="button" <#if isCoOwner && !server.online>onclick="if (confirm('Are you sure?\nThis will overide the minecraft jar!')) call('server', '${server.ID}', 'setVersion', document.getElementById('mcVersionSelector').value);" <#else>disabled</#if> class="btn btn-warning">
        Change MC jar
    </button>
    <hr>
    <div class="form-group">
        <label for="forgeVersionSelector">Install Forge</label>
        <select id="forgeVersionSelector" class="form-control">
        <#list Helper.getForgeNames() as version>
            <option>${version}</option></#list>
        </select>

        <p class="help-block">To see any progress or errors, open the console.</p>
    </div>
    <button type="button" <#if isCoOwner && !server.online>onclick="if (confirm('Are you sure?\nThis will overide the minecraft jar!')) call('server', '${server.ID}', 'installForge', document.getElementById('forgeVersionSelector').value);" <#else>disabled</#if> class="btn btn-warning">
        Install forge
    </button>
    <hr>
    <div class="form-group">
        <label for="modpackURL">Upload modpack zip</label>
        <input id="modpackURL" class="form-control">
    </div>
    <div class="form-group">
        <label for="modpackPurge">
            <input id="modpackPurge" type="checkbox" checked> Purge the server
        </label>
    </div>
    <button type="button" <#if isCoOwner && !server.online>onclick="if (confirm('Are you sure?\nThis will remove and override all files related to this server!')) call('server', '${server.ID}', 'downloadModpack', encodeURIComponent(document.getElementById('modpackURL').value), document.getElementById('modpackPurge').checked);" <#else>disabled</#if> class="btn btn-warning">
        Upload modpack
    </button>
</div>
<style>
    .panel-heading span {
        margin-top: -20px;
        font-size: 15px;
    }

    .row {
        margin-top: 40px;
        padding: 0 10px;
    }

    .clickable {
        cursor: pointer;
    }
</style>
<script>
    const ISCOOWNER = ${(server.isCoOwner(user) || user.isAdmin())?c};

    function openPopup($url) {
        window.open(window.location.origin + $url, '_new', 'height=500,width=800');
    }

    function callOnThisServer(message, func) {
        callOnServer("${server.ID}", message, func);
    }

    function updateInfo (data) {
        document.getElementById("startServerBtn").onclick = !data.online ? function() {callOnThisServer("startServer")} : null;
        document.getElementById("startServerBtn").disabled = data.online;

        document.getElementById("stopServerBtn").onclick = data.online ? function() {callOnThisServer("stopServer|" + prompt('Message?', 'Server is stopping.'))} : null;
        document.getElementById("stopServerBtn").disabled = !data.online;

        document.getElementById("killServerBtn").onclick = data.online ? function() {if (confirm('Are you sure?')) callOnThisServer("forceStopServer")} : null;
        document.getElementById("killServerBtn").disabled = !data.online;

        document.getElementById("onlinePlayers").innerHTML = data.onlinePlayers;
        document.getElementById("slots").innerHTML = data.slots;
        document.getElementById("motd").innerHTML = data.motd;
        document.getElementById("mapName").innerHTML = data.mapName;
        document.getElementById("playerList").innerHTML = data.playerList;
        document.getElementById("version").innerHTML = data.version;
        document.getElementById("serverOwner").innerHTML = data.owner;
        document.getElementById("gameMode").innerHTML = data.gameMode;
        document.getElementById("plugins").innerHTML = data.plugins;
        document.getElementById("gameID").innerHTML = data.gameID;

        document.getElementById("diskspace_server").innerHTML = data.diskspace.server;
        document.getElementById("diskspace_backup").innerHTML = data.diskspace.backups;
        document.getElementById("diskspace_total").innerHTML = data.diskspace.total;

        document.getElementById("serverPortAvailable").innerHTML = data.port_server_available ? "Free" : (data.online ? "In use by us" : "In use by ??");
        document.getElementById("serverPortAvailable").className = "label label-" + (data.port_server_available ? "success" : (data.online ? "warning" : "danger"));
        document.getElementById("rconPortAvailable").innerHTML = data.port_rcon_available ? "Free" : (data.online ? "In use by us" : "In use by ??");
        document.getElementById("rconPortAvailable").className = "label label-" + (data.port_rcon_available ? "success" : (data.online ? "warning" : "danger"));

        document.getElementById("online").innerHTML = data.online ? "Online" : "Offline";
        document.getElementById("online").className = "label label-" + (data.online ? "success" : "danger");
    }

    var websocket = new WebSocket(wsurl("servermonitor/${server.ID}"));
    websocket.onmessage = function (evt)
    {
        var temp = JSON.parse(evt.data);
        console.log(temp.data);
        if (temp.status === "ok") updateInfo(temp.data);
        else alert(temp.message);
    }
    websocket.onerror =  function (evt) { alert("The websocket errored. Refresh the page!") }
    websocket.onclose =  function (evt) { alert("The websocket closed. Refresh the page!") }
</script>
<#include "footer.ftl">
