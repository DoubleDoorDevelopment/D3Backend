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
                    Server owner is <span id="serverOwner"></span>.<br />
                    Server port status: <span id="serverPortAvailable"></span><br />
                    Server uptime: ${server.online?string(Helper.getOnlineTime(server.startTime, "%2d days, ", "%2d hours, ", "%2d min and ", "%2d sec"), "offline")}<br />
                </p>
                <hr />
                <p>
                    Diskspace in use:<br>
                    by server: <span id="diskspace_server"></span>MB<br />
                    by backups: <span id="diskspace_backup"></span>MB<br />
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
                    <#--<a type="button" class="btn btn-default" href="/worldmanager?server=${server.ID?js_string}">World Manager</a>-->
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
                <hr>
                <h4>Security log</h4>
                <textarea title="Security log" id="security-log" class="form-control" rows="4" readonly>${server.actionLog?reverse?join('\n')}</textarea>
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
                <label for="mcVersionSelector">
                    Minecraft version
                </label>
                <select id="mcVersionSelector" class="form-control">
                <#list Helper.getAllMCVersions() as version >
                    <option>${version}</option>
                <#else>
                    <option>Data not yet downloaded...</option>
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
                <label for="forgeVersionSelectorMinecraft">
                    Minecraft version
                </label>
                <select id="forgeVersionSelectorMinecraft" class="form-control" onchange="updateForges(this.value)">
                    <option>Loading data...</option>
                </select>
                <br>
                <label for="forgeVersionSelectorForge">
                    Forge version
                </label>
                <select id="forgeVersionSelectorForge" class="form-control">
                    <option>Loading data...</option>
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
                <div class="form-group">
                    <input id="modpackURL" class="form-control" placeholder="URL">
                    <input type="file" id="modpackFile" name="fileName">
                    <p class="help-block">Pick either a URL or a direct file.</p>
                </div>
                <label for="modpackPurge">
                    <input id="modpackPurge" type="checkbox"> Purge the server
                </label>
                <label for="modpackCurse">
                    <input id="modpackCurse" type="checkbox"> This is a <small>CurseForge/</small>Twitch zip <small>(Client side only!)</small>
                </label>
                <br>
                <button type="button" <#if isCoOwner && !server.online>onclick="packUpload()" <#else>disabled</#if> class="btn btn-warning">
                    Install Modpack
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
                <button type="button" class="btn btn-default" data-dismiss="modal" id="modal-close">Close</button>
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
        if (confirm('Are you sure?\nThis will override the minecraft jar!'))
        {
            get('modalLabel').innerHTML = 'Installing MC ' + get('mcVersionSelector').value;
            call('servercmd/${server.ID?js_string}', 'setVersion', [get('mcVersionSelector').value], progressModal);
        }
    }

    function installForge()
    {
        if (confirm('Are you sure?\nThis will override the minecraft jar!'))
        {
            get('modalLabel').innerHTML = 'Installing Forge ' + get('forgeVersionSelectorForge').value;
            call('servercmd/${server.ID?js_string}', 'installForge', [get('forgeVersionSelectorMinecraft').value, get('forgeVersionSelectorForge').value], progressModal);
        }
    }

    function packUpload()
    {
        if (confirm('Are you sure?\nThis will override the minecraft jar!\nPurge server: ' + get('modpackPurge').checked + '\nCurse Pack: ' + get('modpackCurse').checked))
        {
            get('modalLabel').innerHTML = 'Uploading modpack...';

            var file = $('#modpackFile')[0].files[0];
            if (file)
            {
                var formData = new FormData();
                formData.append('fileName', file);

                $.ajax({
                    url: window.location.pathname + window.location.search,
                    type: "POST",
                    data: formData,
                    contentType: false,
                    cache: false,
                    processData: false,
                    beforeSend: function()
                    {
                        progressModal("Starting file upload...");
                    },
                    success: function()
                    {
                        progressModal("Done uploading file.\nStarting modpack install...");
                        call('servercmd/${server.ID?js_string}', 'installModpack', [
                            file.name,
                            get('modpackPurge').checked,
                            get('modpackCurse').checked], progressModal);
                    },
                    error: function()
                    {
                        progressModal("Error uploading modpack...");
                    }
                });
            }
            else
            {
                call('servercmd/${server.ID?js_string}', 'downloadModpack', [
                    get('modpackURL').value,
                    get('modpackPurge').checked,
                    get('modpackCurse').checked], progressModal);
            }
        }
    }

    var modal = $('#modal');
    var needsShowing = true;
    function progressModal(data)
    {
        if (needsShowing)
        {
            get("modal-close").className = "btn btn-default";
            modal.modal("show");
            get("modal-log").innerHTML = "";
            needsShowing = false;
        }
        if (data === "done")
        {
            needsShowing = true;
            get("modal-log").innerHTML += "-- ALL DONE --\n";
            get("modal-close").className = "btn btn-success";
        }
        else
        {
            get("modal-log").innerHTML += data + "\n";
        }
    }

    function updateInfo(data)
    {
        if (data.hasOwnProperty("deleted") && data['deleted'])
        {
            document.location = "/servers";
        }

        get("startServerBtn").onclick = !data.online ? function ()
        {
            call('servercmd/${server.ID?js_string}', "startServer")
        } : null;
        get("startServerBtn").disabled = data.online;

        get("stopServerBtn").onclick = data.online ? function ()
        {
            call('servercmd/${server.ID?js_string}', "stopServer", [prompt('Message?', 'Server is stopping.')])
        } : null;
        get("stopServerBtn").disabled = !data.online;

        get("killServerBtn").onclick = data.online ? function (e)
        {
            if (!!e.shiftKey) {
                if (confirm('Are you sure you want to FORCE kill?\nTry a regular kill first!'))
                    call('servercmd/${server.ID?js_string}', "murderServer")
            }
            else if (confirm('Are you sure?')) call('servercmd/${server.ID?js_string}', "forceStopServer")
        } : null;
        get("killServerBtn").disabled = !data.online;

        get("onlinePlayers").innerHTML = data.onlinePlayers;
        get("slots").innerHTML = data.slots;
        get("motd").innerHTML = data.motd;
        get("mapName").innerHTML = data.mapName;
        get("playerList").innerHTML = data.playerList;
        get("version").innerHTML = data.version;
        get("serverOwner").innerHTML = data.owner;
        get("gameMode").innerHTML = data.gameMode;
        get("plugins").innerHTML = data.plugins;
        get("gameID").innerHTML = data.gameID;
        get("ram").innerHTML = data.ram;

        get("diskspace_server").innerHTML = data.diskspace.server;
        get("diskspace_backup").innerHTML = data.diskspace.backups;
        get("diskspace_total").innerHTML = data.diskspace.total;

        get("serverPortAvailable").innerHTML = data.port_server_available ? "Free" : (data.online ? "In use by us" : "In use by ??");
        get("serverPortAvailable").className = "label label-" + (data.port_server_available ? "success" : (data.online ? "warning" : "danger"));

        get("online").innerHTML = data.online ? "Online" : "Offline";
        get("online").className = "label label-" + (data.online ? "success" : "danger");

        get("coOwnersList").innerHTML = "";
        var tmp = "";
        data.coOwners.forEach(function (entry)
        {
            tmp += "<li>" + entry + "<#if isOwner><i style=\"cursor: pointer;\" onclick=\"call('servercmd/${server.ID?js_string}', 'removeCoowner', ['" + entry + "'])\" class=\"fa fa-times\"></i></#if></li>";
        });
        get("coOwnersList").innerHTML = tmp;

        get("adminsList").innerHTML = "";
        tmp = "";
        data.admins.forEach(function (entry)
        {
            tmp += "<li>" + entry + "<#if isOwner><i style=\"cursor: pointer;\" onclick=\"call('servercmd/${server.ID?js_string}', 'removeAdmin', ['" + entry + "'])\" class=\"fa fa-times\"></i></#if></li>";
        });
        get("adminsList").innerHTML = tmp;
    }
    websocketMonitor = new WebSocket(wsurl("servermonitor/${server.ID?js_string}"));
    websocketMonitor.onerror = function (evt)
    {
        addAlert("The websocket errored. Refresh the page!")
    };
    websocketMonitor.onclose = function (evt)
    {
        addAlert("The websocket closed. Refresh the page!")
    };
    websocketMonitor.onmessage = function (evt)
    {
        var temp = JSON.parse(evt.data);
        if (temp.status === "ok")
        {
            if (typeof temp.data === "string")
            {
                get("security-log").innerHTML = temp.data + "\n" + get("security-log").innerHTML;
            }
            else
            {
                updateInfo(temp.data);
            }
        }
        else
        {
            addAlert(temp.message);
        }
    };

    $(function () {
        get("forgeVersionSelectorMinecraft").innerHTML = "";
        var tmpMC = "";
        for (var i in forgeVersions) {
            if (forgeVersions.hasOwnProperty(i)) {
                tmpMC += "<option>" + i + "</option>";
            }
        }
        if (tmpMC.length === 0) tmpMC = "<option>Forge data not finished downloading.</option>";
        else updateForges(Object.keys(forgeVersions)[0]);
        get("forgeVersionSelectorMinecraft").innerHTML = tmpMC;
    });

    function updateForges(val)
    {
        var forges = forgeVersions[val];
        get("forgeVersionSelectorForge").innerHTML = "";
        var tmpForge = "";
        for (var i in forges) {
            if (forges.hasOwnProperty(i)) {
                tmpForge += "<option>" + forges[i] + "</option>";
            }
        }
        get("forgeVersionSelectorForge").innerHTML = tmpForge;
    }

    var forgeVersions = ${Helper.getForgeVersionJson()};
</script>
<#include "footer.ftl">
