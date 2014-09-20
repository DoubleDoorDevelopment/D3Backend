<#include "header.ftl">
<h1>Server: ${server.name}    <small><span class="label label-<#if server.online>success<#else>danger</#if>"><#if server.online>Online<#else>Offline</#if></span></small></h1>
<div class="row">
    <div class="col-md-4">
        <div class="panel panel-info">
            <div class="panel-heading">
                <h3 class="panel-title" style="text-align: center;">Server information</h3>
            </div>
            <div class="panel-body" style="text-align: center;">
                <p>
                    Server port status: <span class="label label-<#if server.isPortAvailable(server.serverPort)>success<#elseif server.online>warning<#else>danger</#if>"><#if server.isPortAvailable(server.serverPort)>Free<#elseif server.online>In use by us<#else>In use by ?</#if></span><br>
                    RCon port status: <span class="label label-<#if server.isPortAvailable(server.rconPort)>success<#elseif server.online>warning<#else>danger</#if>"><#if server.isPortAvailable(server.rconPort)>Free<#elseif server.online>In use by us<#else>In use by ?</#if></span>
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
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="panel panel-info">
            <div class="panel-heading">
                <h3 class="panel-title" style="text-align: center;">Server control</h3>
            </div>
            <div class="panel-body" style="text-align: center;">
                <div class="btn-group">
                    <button type="button" <#if !server.online>onclick="call('server', '${server.name}', 'startServer')"<#else>disabled</#if> class="btn btn-success">Start</button>
                    <button type="button" class="btn btn-info" onclick="openPopup('/console/${server.name}')">Console</button>
                    <button type="button" <#if server.online>onclick="call('server', '${server.name}', 'stopServer')"<#else>disabled</#if> class="btn btn-warning">Stop</button>
                    <button type="button" <#if server.online>onclick="if (confirm('Are you sure?')) call('server', '${server.name}', 'forceStopServer');"<#else>disabled</#if> class="btn btn-danger">Kill</button>
                </div>
                <form class="form-horizontal" role="form" style="padding-left: 10px; padding-right: 10px;">
                    <div class="checkbox">
                        <label>
                            <input type="checkbox" onclick="call('server', '${server.name}', 'setAutoStart', this.checked)" <#if server.autoStart>checked</#if>> Autostart
                        </label>
                    </div>
                    <br>
                    <div class="form-group">
                        <div class="input-group">
                            <div class="input-group-addon">RAM min</div>
                            <input class="form-control" type="number" name="quantity" placeholder="1024" value="${server.ramMin?c}" <#if !server.online>onchange="call('server', '${server.name}', 'setRamMin', this.value)"<#else>disabled</#if>>
                        </div>
                    </div>
                    <div class="form-group">
                        <div class="input-group">
                            <div class="input-group-addon">RAM max</div>
                            <input class="form-control" type="number" name="quantity" placeholder="1024" value="${server.ramMax?c}" <#if !server.online>onchange="call('server', '${server.name}', 'setRamMax', this.value)"<#else>disabled</#if>>
                        </div>
                    </div>
                    <div class="form-group">
                        <div class="input-group">
                            <div class="input-group-addon">perm Gen</div>
                            <input class="form-control" type="number" name="quantity" placeholder="128" value="${server.permGen?c}" <#if !server.online>onchange="call('server', '${server.name}', 'setPermGen', this.value)"<#else>disabled</#if>>
                        </div>
                    </div>
                    <div class="form-group">
                        <div class="input-group">
                            <div class="input-group-addon">Jar</div>
                            <input class="form-control" placeholder="minecraft_server.jar" value="${server.jarName}" <#if !server.online>onchange="call('server', '${server.name}', 'setJarName', this.value)"<#else>disabled</#if>>
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="ExtraJavaParameters">Extra Java Parameters</label>
                        <textarea id="ExtraJavaParameters" class="form-control" rows="3" <#if !server.online>onchange="call('server', '${server.name}', 'setExtraJavaParameters', this.value.split('\n'))"<#else>disabled</#if>>${server.extraJavaParameters?join("\n")}</textarea>
                        <p class="help-block">One per line.</p>
                    </div>
                    <div class="form-group">
                        <label for="ExtraServerParameters">Extra Server Parameters</label>
                        <textarea id="ExtraServerParameters" class="form-control" rows="3" <#if !server.online>onchange="call('server', '${server.name}', 'setExtraMCParameters', this.value.split('\n'))"<#else>disabled</#if>>${server.extraMCParameters?join("\n")}</textarea>
                        <p class="help-block">One per line.</p>
                    </div>
                </form>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="panel panel-info">
            <div class="panel-heading">
                <h3 class="panel-title" style="text-align: center;">Server Properties File</h3>
            </div>
            <div class="panel-body" style="text-align: center;">
                <p>No checks are preformed when submitting. Be careful!</p>
                <form class="form-horizontal" role="form" style="padding-left: 10px; padding-right: 10px;">
                    <textarea id="serverProperties" class="form-control" rows="25" <#if server.online>disabled</#if>>${server.propertiesAsText}</textarea>
                    <br>
                    <button class="btn btn-primary btn-lg btn-block" <#if !server.online>onclick="call('server', '${server.name}', 'setPropertiesAsText', encodeURIComponent(document.getElementById('serverProperties').value))"<#else>disabled</#if>>Send!</button>
                </form>
            </div>
        </div>
    </div>
</div>
<script>
    function openPopup($url)
    {
        window.open(window.location.origin + $url, '_new', 'height=500,width=800');
    }
</script>
<#include "footer.ftl">
