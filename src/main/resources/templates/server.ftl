<#include "header.ftl">
<#assign allowModify = server.canUserControl(user) >
<#assign isCoOwner = server.isCoOwner(user) >
<h1>${server.name} <small> ${server.getDisplayAddress()}   <span class="label label-<#if server.online>success<#else>danger</#if>"><#if server.online>Online<#else>Offline</#if></span></small></h1>
<p>
    <div class="btn-group">
        <button type="button" <#if allowModify && !server.online>onclick="call('server', '${server.name}', 'startServer')" <#else>disabled</#if> class="btn btn-success">Start</button>
        <button type="button" class="btn btn-info" <#if allowModify>onclick="openPopup('/console/${server.name}')" <#else>disabled</#if>>Console</button>
        <button type="button" <#if allowModify && server.online>onclick="call('server', '${server.name}', 'stopServer')" <#else>disabled</#if> class="btn btn-warning">Stop</button>
        <button type="button" <#if allowModify && server.online>onclick="if (confirm('Are you sure?')) call('server', '${server.name}', 'forceStopServer');" <#else>disabled</#if> class="btn btn-danger">Kill</button>
    </div>
</p>
<div class="row">
    <div class="col-sm-6">
        <div class="panel panel-info">
            <div class="panel-heading">
                <h3 class="panel-title" style="text-align: center;">Server information</h3>
                <span class="pull-right clickable"><i class="fa fa-chevron-up"></i></span>
            </div>
            <div class="panel-body" style="text-align: center;">
                <p>
                    Server port status: <span class="label label-<#if Helper.isPortAvailable(server.ip, server.serverPort)>success<#elseif server.online>warning<#else>danger</#if>"><#if Helper.isPortAvailable(server.ip, server.serverPort)> Free<#elseif server.online>In use by us<#else>In use by ?</#if></span><br>
                    RCon port status: <span class="label label-<#if Helper.isPortAvailable(server.ip, server.rconPort)>success<#elseif server.online>warning<#else>danger</#if>"><#if Helper.isPortAvailable(server.ip, server.rconPort)> Free<#elseif server.online>In use by us<#else>In use by ?</#if></span>
                </p>
                <table class="table table-hover table-condensed">
                    <thead>
                    <tr>
                        <th style="text-align: right;width: 33%">Property</th>
                        <th style="text-align: left;">Value</th>
                    </tr>
                    </thead>
                    <tbody>
                    <#list ["name", "onlinePlayers", "slots", "motd", "gameMode", "mapName", "playerList", "plugins", "version", "gameID"] as key>
                        <#assign value = server.get(key)>
                    <tr>
                        <td style="text-align: right;">${key}</td>
                        <td style="text-align: left;"><#if value?is_number>${value?c}<#elseif value?is_boolean>${value?string}<#elseif value?is_sequence>${value?join(", ")}<#else>${value}</#if></td>
                    </tr>
                    </#list>
                    </tbody>
                </table>
                <p>Diskspace in use by server + backups: ${server.diskspaceUse} MB</p>
            </div>
        </div>
    </div>
    <div class="col-sm-6">
        <div class="panel panel-info">
            <div class="panel-heading">
                <h3 class="panel-title" style="text-align: center;">Server control</h3>
                <span class="pull-right clickable"><i class="fa fa-chevron-up"></i></span>
            </div>
            <div class="panel-body" style="text-align: center;">
                <form class="form-horizontal" role="form" style="padding-left: 10px; padding-right: 10px;">
                    <div class="checkbox">
                        <label>
                            <input type="checkbox" <#if allowModify>onclick="call('server', '${server.name}', 'setAutoStart', this.checked)" <#else>disabled</#if> <#if server.autoStart>checked</#if>> Autostart
                        </label>
                    </div>
                    <br>

                    <div class="form-group">
                        <div class="input-group">
                            <div class="input-group-addon">RAM min</div>
                            <input class="form-control" type="number" name="quantity" placeholder="1024" value="${server.ramMin?c}" <#if isCoOwner && !server.online>onchange="call('server', '${server.name}', 'setRamMin', this.value)" <#else>disabled</#if>>
                        </div>
                    </div>
                    <div class="form-group">
                        <div class="input-group">
                            <div class="input-group-addon">RAM max</div>
                            <input class="form-control" type="number" name="quantity" placeholder="1024" value="${server.ramMax?c}" <#if isCoOwner && !server.online>onchange="call('server', '${server.name}', 'setRamMax', this.value)" <#else>disabled</#if>>
                        </div>
                    </div>
                    <div class="form-group">
                        <div class="input-group">
                            <div class="input-group-addon">perm Gen</div>
                            <input class="form-control" type="number" name="quantity" placeholder="128" value="${server.permGen?c}" <#if isCoOwner && !server.online>onchange="call('server', '${server.name}', 'setPermGen', this.value)" <#else>disabled</#if>>
                        </div>
                    </div>
                    <div class="form-group">
                        <div class="input-group">
                            <div class="input-group-addon">Jar</div>
                            <input class="form-control" placeholder="minecraft_server.jar" value="${server.jarName}" <#if isCoOwner && !server.online>onchange="call('server', '${server.name}', 'setJarName', this.value)" <#else>disabled</#if>>
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="ExtraJavaParameters">Extra Java Parameters</label>
                        <textarea id="ExtraJavaParameters" class="form-control" rows="3" <#if isCoOwner && !server.online>onchange="call('server', '${server.name}', 'setExtraJavaParameters', this.value.split('\n'))" <#else>disabled</#if>>${server.extraJavaParameters?join("\n")}
                        </textarea>
                        <p class="help-block">One per line.</p>
                    </div>
                    <div class="form-group">
                        <label for="ExtraServerParameters">Extra Server Parameters</label>
                        <textarea id="ExtraServerParameters" class="form-control" rows="3" <#if isCoOwner && !server.online>onchange="call('server', '${server.name}', 'setExtraMCParameters', this.value.split('\n'))" <#else>disabled</#if>>${server.extraMCParameters?join("\n")}</textarea>
                        <p class="help-block">One per line.</p>
                    </div>
                </form>
            </div>
        </div>
    </div>
    <div class="col-sm-6">
        <div class="panel panel-info">
            <div class="panel-heading">
                <h3 class="panel-title" style="text-align: center;">Server Properties File</h3>
                <span class="pull-right clickable"><i class="fa fa-chevron-up"></i></span>
            </div>
            <div class="panel-body" style="text-align: center;">
                <p>No checks are preformed when submitting. Be careful!</p>
                <form class="form-horizontal" role="form" style="padding-left: 10px; padding-right: 10px;" method="post">
                    <textarea id="serverProperties" name="serverProperties" class="form-control" rows="25" <#if isCoOwner && server.online>disabled</#if>>${server.propertiesAsText}</textarea>
                    <br>
                    <button class="btn btn-primary btn-lg btn-block" <#if isCoOwner && !server.online>type="submit" <#else>disabled</#if>>Send!
                    </button>
                </form>
            </div>
        </div>
    </div>
    <div class="col-sm-6">
        <div class="panel panel-danger">
            <div class="panel-heading">
                <h3 class="panel-title" style="text-align: center;">Danger zone&#x2122;</h3>
                <span class="pull-right clickable"><i class="fa fa-chevron-up"></i></span>
            </div>
            <div class="panel-body" style="text-align: center;">
                <div class="btn-group">
                    <button type="button" <#if isCoOwner && !server.online>onclick="if (confirm('Are you sure?\nThis will remove all files related to this server!')) {call('server', '${server.name}', 'delete'); window.location='/'}" <#else>disabled</#if> class="btn btn-danger">Delete server</button>
                    <a type="button" href='/worldmanager/${server.name}' class="btn btn-info">World Manager</a>
                </div>
                <hr>
                <div class="form-group">
                    <label for="Admins">Co Owners</label>
                    <textarea id="Admins" class="form-control" rows="3" <#if server.owner == user.username>onchange="call('server', '${server.name}', 'setCoOwners', this.value.split('\n'))" <#else>disabled</#if>>${server.coOwners?join("\n")}</textarea>
                    <p class="help-block">Usernames on the backend. One per line. Can do everything except modify Co Owners</p>
                </div>
                <hr>
                <div class="form-group">
                    <label for="Admins">Admins</label>
                    <textarea id="Admins" class="form-control" rows="3" <#if isCoOwner>onchange="call('server', '${server.name}', 'setAdmins', this.value.split('\n'))" <#else>disabled</#if>>${server.admins?join("\n")}</textarea>
                    <p class="help-block">Usernames on the backend. One per line. Can start, stop & use console.</p>
                </div>
                <hr>
                <div class="form-group">
                    <label for="mcVersionSelector">MC jar version</label>
                    <select id="mcVersionSelector" class="form-control">
                    <#list Helper.getAllMCVersions() as version><option>${version}</option></#list>
                    </select>
                    <p class="help-block">To see any progress or errors, open the console.</p>
                </div>
                <button type="button" <#if isCoOwner && !server.online>onclick="if (confirm('Are you sure?\nThis will overide the minecraft jar!')) call('server', '${server.name}', 'setVersion', document.getElementById('mcVersionSelector').value);" <#else>disabled</#if> class="btn btn-warning">Change MC jar</button>
                <hr>
                <hr>
                <div class="form-group">
                    <label for="forgeVersionSelector">Install Forge</label>
                    <select id="forgeVersionSelector" class="form-control">
                    <#list Helper.getForgeNames() as version><option>${version}</option></#list>
                    </select>
                    <p class="help-block">To see any progress or errors, open the console.</p>
                </div>
                <button type="button" <#if isCoOwner && !server.online>onclick="if (confirm('Are you sure?\nThis will overide the minecraft jar!')) call('server', '${server.name}', 'installForge', document.getElementById('forgeVersionSelector').value);" <#else>disabled</#if> class="btn btn-warning">Install forge</button>
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
                <button type="button" <#if isCoOwner && !server.online>onclick="if (confirm('Are you sure?\nThis will remove and override all files related to this server!')) call('server', '${server.name}', 'downloadModpack', encodeURIComponent(document.getElementById('modpackURL').value), document.getElementById('modpackPurge').checked);" <#else>disabled</#if> class="btn btn-warning">Upload modpack</button>
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
</style>
<script>
    function openPopup($url) {
        window.open(window.location.origin + $url, '_new', 'height=500,width=800');
    }
</script>
<#include "footer.ftl">
