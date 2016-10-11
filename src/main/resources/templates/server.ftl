<#include "header.ftl">
<#assign isCoOwner = server.isCoOwner(user) || user.isAdmin() >
<#assign isOwner = server.ownerObject == user || user.isAdmin() >
<h1>${server.ID?js_string}
    <small> ${server.getDisplayAddress()}   <span id="online"></span></small>
</h1>
<div class="btn-group">
    <button type="button" id="startServerBtn" class="btn btn-success">Start</button>
    <button type="button" class="btn btn-info" onclick="openPopup('/serverconsole?server=${server.ID?js_string}')">Console</button>
    <button type="button" id="stopServerBtn" class="btn btn-warning">Stop</button>
    <button type="button" id="killServerBtn" class="btn btn-danger" title="Hold shift to Force Kill. Only use as a last resort.">Kill</button>
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
                    Server uptime: ${server.online?string(Helper.getOnlineTime(server.startTime, "%2d days, ", "%2d hours, ", "%2d min and ", "%2d sec"), "offline")}<br>
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
                    <#list ["onlinePlayers", "slots", "motd", "gameMode", "mapName", "playerList", "plugins", "version", "gameID", "ram"] as key>
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
                    <a type="button" class="btn btn-default" href='/filemanager?server=${server.ID?js_string}'>File Manager</a>
                    <a type="button" class="btn btn-default" href="/filemanager?server=${server.ID?js_string}&file=server.properties">server.properties</a>
                    <a type="button" class="btn btn-default" href="/worldmanager?server=${server.ID?js_string}">World Manager</a>
                </div>
                <hr>
                <a type="button" class="btn btn-warning" href="/advancedsettings?server=${server.ID?js_string}">Advanced Settings</a>
                <hr>
                <div class="btn-group">
                    <button type="button" <#if isOwner>onclick="var name = prompt('Username of the future owner?'); if (name != null && confirm('Are you sure?')) {call('servercmd/${server.ID?js_string}', 'setOwner', [name]);}"<#else>disabled</#if> class="btn btn-danger">
                        Change owner
                    </button>
                    <button type="button" <#if isOwner && !server.online>onclick="if (confirm('Are you sure?\nThis will remove all files related to this server!')) {call('servercmd/${server.ID?js_string}', 'delete', [], function() {window.location='/servers'});}"<#else>disabled</#if> class="btn btn-danger">
                        Delete server
                    </button>
                </div>
                <hr>
                <h4>Co-owners <#if isOwner>
                    <small style="cursor: pointer;" onclick="var name = prompt('Username of the future co-owner?');if (name != null && name !== '') {call('servercmd/${server.ID?js_string}', 'addCoowner', [name])}"><i class="fa fa-plus"></i>
                    </small></#if></h4>
                <ul class="list-unstyled" id="coOwnersList">
                <#list server.getCoOwners() as name>
                    <li>${name}<#if isOwner><i style="cursor: pointer;" onclick="call('servercmd/${server.ID?js_string}', 'removeCoowner', ['${name}'])" class="fa fa-times"></i></#if></li>
                </#list>
                </ul>
                <p class="text-muted">Usernames on the backend. Can do everything except modify co-owners, change owner and delete the server.</p>
                <hr>
                <h4>Admins <#if isCoOwner>
                    <small style="cursor: pointer;" onclick="var name = prompt('Username of the future admin?');if (name != null && name !== '') {call('servercmd/${server.ID?js_string}', 'addAdmin', [name])}"><i class="fa fa-plus"></i></small></#if>
                </h4>
                <ul class="list-unstyled" id="adminsList">
                <#list server.getAdmins() as name>
                    <li>${name}<#if isOwner><i style="cursor: pointer;" onclick="call('servercmd/${server.ID?js_string}', 'removeAdmin', ['${name}'])" class="fa fa-times"></i></#if></li>
                </#list>
                </ul>
                <p class="text-muted">Usernames on the backend. Can start, stop & use console.</p>
            </div>
        </div>
    </div>
</div>
<div class="row">
    <div class="col-sm-4">
        <div class="panel panel-info">
            <div class="panel-heading">
                <h3 class="panel-title" style="text-align: center;">MC jar installer</h3>
            </div>
            <div class="panel-body" style="text-align: center;">
                <select id="mcVersionSelector" class="form-control">
                <#list Helper.getAllMCVersions() as version>
                    <option>${version}</option>
                </#list>
                </select>
                <br>
                <button type="button" <#if isCoOwner && !server.online>onclick="changeMCJar()" <#else>disabled</#if> class="btn btn-warning">
                    Change MC jar
                </button>
            </div>
        </div>
    </div>
    <div class="col-sm-4">
        <div class="panel panel-info">
            <div class="panel-heading">
                <h3 class="panel-title" style="text-align: center;">Install Forge</h3>
            </div>
            <div class="panel-body" style="text-align: center;">
                <select id="forgeVersionSelector" class="form-control">
                <#list Helper.getForgeNames() as version>
                    <option>${version}</option>
                </#list>
                </select>
                <br>
                <button type="button" <#if isCoOwner && !server.online>onclick="installForge()" <#else>disabled</#if> class="btn btn-warning">
                    Install forge
                </button>
            </div>
        </div>
    </div>
    <div class="col-sm-4">
        <div class="panel panel-info">
            <div class="panel-heading">
                <h3 class="panel-title" style="text-align: center;">Upload modpack zip</h3>
            </div>
            <div class="panel-body" style="text-align: center;">
                <input id="modpackURL" class="form-control" placeholder="URL here">
                <label for="modpackPurge">
                    <input id="modpackPurge" type="checkbox"> Purge the server
                </label>
                <label for="modpackCurse">
                    <input id="modpackCurse" type="checkbox"> This is a curse modpack zip
                </label>
                <br>
                <button type="button" <#if isCoOwner && !server.online>onclick="packUpload()" <#else>disabled</#if> class="btn btn-warning">
                    Upload modpack
                </button>
            </div>
        </div>
    </div>
</div>
<!-- Modal -->
<div class="modal fade" id="modal" tabindex="-1" role="dialog" aria-labelledby="modalLabel" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">&times;</span><span class="sr-only">Close</span></button>
                <h4 class="modal-title" id="modalLabel">Operation in progress</h4>
            </div>
            <div class="modal-body" id="modal-body">
                <pre id="modal-log"></pre>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
            </div>
        </div>
    </div>
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

    #modal-log {
        overflow: auto;
        max-height: 60vh;
        word-wrap: normal;
        white-space: pre;
    }
</style>
<script>
    function openPopup($url)
    {
        window.open(window.location.origin + $url, '_new', 'height=500,width=800');
    }

    function changeMCJar()
    {
        if (confirm('Are you sure?\nThis will overide the minecraft jar!'))
        {
            document.getElementById('modalLabel').innerHTML = 'Installing MC ' + document.getElementById('mcVersionSelector').value;
            call('servercmd/${server.ID?js_string}', 'setVersion', [document.getElementById('mcVersionSelector').value], progressModal);
        }
    }

    function installForge()
    {
        if (confirm('Are you sure?\nThis will overide the minecraft jar!'))
        {
            document.getElementById('modalLabel').innerHTML = 'Installing Forge ' + document.getElementById('forgeVersionSelector').value;
            call('servercmd/${server.ID?js_string}', 'installForge', [document.getElementById('forgeVersionSelector').value], progressModal);
        }
    }

    function packUpload()
    {
        if (confirm('Are you sure?\nThis will overide the minecraft jar!\nPurge server: ' + document.getElementById('modpackPurge').checked + '\nCurse Pack: ' + document.getElementById('modpackCurse').checked))
        {
            document.getElementById('modalLabel').innerHTML = 'Uploading modpack: ' + document.getElementById('modpackURL').value;
            call('servercmd/${server.ID?js_string}', 'downloadModpack', [
                document.getElementById('modpackURL').value,
                document.getElementById('modpackPurge').checked,
                document.getElementById('modpackCurse').checked], progressModal);
        }
    }

    var modal = $('#modal');
    var needsShowing = true;
    function progressModal(data)
    {
        if (needsShowing)
        {
            modal.modal("show");
            document.getElementById("modal-log").innerHTML = "";
            needsShowing = false;
        }
        if (data === "done")
        {
            needsShowing = true;
            document.getElementById("modal-log").innerHTML += "-- ALL DONE --\n";
        }
        document.getElementById("modal-log").innerHTML += data + "\n";
    }

    function updateInfo(data)
    {
        document.getElementById("startServerBtn").onclick = !data.online ? function ()
        {
            call('servercmd/${server.ID?js_string}', "startServer")
        } : null;
        document.getElementById("startServerBtn").disabled = data.online;

        document.getElementById("stopServerBtn").onclick = data.online ? function ()
        {
            call('servercmd/${server.ID?js_string}', "stopServer", [prompt('Message?', 'Server is stopping.')])
        } : null;
        document.getElementById("stopServerBtn").disabled = !data.online;

        document.getElementById("killServerBtn").onclick = data.online ? function (e)
        {
            if (!!e.shiftKey) {
                if (confirm('Are you sure you want to FORCE kill?\nTry a regular kill first!'))
                    call('servercmd/${server.ID?js_string}', "murderServer")
            }
            else if (confirm('Are you sure?')) call('servercmd/${server.ID?js_string}', "forceStopServer")
        } : null;
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
        document.getElementById("ram").innerHTML = data.ram;

        document.getElementById("diskspace_server").innerHTML = data.diskspace.server;
        document.getElementById("diskspace_backup").innerHTML = data.diskspace.backups;
        document.getElementById("diskspace_total").innerHTML = data.diskspace.total;

        document.getElementById("serverPortAvailable").innerHTML = data.port_server_available ? "Free" : (data.online ? "In use by us" : "In use by ??");
        document.getElementById("serverPortAvailable").className = "label label-" + (data.port_server_available ? "success" : (data.online ? "warning" : "danger"));

        document.getElementById("online").innerHTML = data.online ? "Online" : "Offline";
        document.getElementById("online").className = "label label-" + (data.online ? "success" : "danger");

        document.getElementById("coOwnersList").innerHTML = "";
        data.coOwners.forEach(function (entry)
        {
            document.getElementById("coOwnersList").innerHTML += "<li>" + entry + "<#if isOwner><i style=\"cursor: pointer;\" onclick=\"call('servercmd/${server.ID?js_string}', 'removeCoowner', ['" + entry + "'])\" class=\"fa fa-times\"></i></#if></li>";
        });

        document.getElementById("adminsList").innerHTML = "";
        data.admins.forEach(function (entry)
        {
            document.getElementById("adminsList").innerHTML += "<li>" + entry + "<#if isOwner><i style=\"cursor: pointer;\" onclick=\"call('servercmd/${server.ID?js_string}', 'removeAdmin', ['" + entry + "'])\" class=\"fa fa-times\"></i></#if></li>";
        });
    }
    websocketMonitor = new WebSocket(wsurl("servermonitor/${server.ID?js_string}"));
    websocketMonitor.onerror = function (evt)
    {
        alert("The websocket errored. Refresh the page!")
    };
    websocketMonitor.onclose = function (evt)
    {
        alert("The websocket closed. Refresh the page!")
    };
    websocketMonitor.onmessage = function (evt)
    {
        var temp = JSON.parse(evt.data);
        if (temp.status === "ok")
        {
            updateInfo(temp.data);
        }
        else
        {
            alert(temp.message);
        }
    };
</script>
<#include "footer.ftl">
