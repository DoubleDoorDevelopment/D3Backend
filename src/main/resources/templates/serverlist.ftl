<#include "header.ftl">
<h1>Server list</h1>
<table class="table table-hover">
    <thead>
    <tr>
        <th class="col-sm-2">Server Name</th>
        <th class="col-sm-2">Server Address</th>
        <th class="col-sm-1">Players</th>
        <th class="col-sm-1">Size</th>
        <th class="col-sm-3"></th>
        <th class="col-sm-3">MOTD</th>
    </tr>
    </thead>
    <tbody>
    <#list Settings.servers as server>
        <#if server.canUserControl(user)>
            <tr class="<#if server.online>success<#else>danger</#if>" style="cursor:pointer;">
                <td onclick="window.document.location='/servers/${server.name}'">${server.name}</td>
                <td onclick="window.document.location='/servers/${server.name}'">${server.displayAddress}</td>
                <td onclick="window.document.location='/servers/${server.name}'">${server.onlinePlayers}/${server.slots}</td>
                <td onclick="window.document.location='/servers/${server.name}'">${server.diskspaceUse} MB</td>
                <td>
                    <div class="btn-group">
                        <button type="button" <#if !server.online>onclick="call('server', '${server.name}', 'startServer')" <#else>disabled</#if> class="btn btn-success btn-xs">Start</button>
                        <button type="button" class="btn btn-info btn-xs" onclick="openPopup('/console/${server.name}')">Console</button>
                        <button type="button" <#if server.online>onclick="call('server', '${server.name}', 'stopServer')" <#else>disabled</#if> class="btn btn-warning btn-xs">Stop</button>
                        <button type="button" <#if server.online>onclick="if (confirm('Are you sure?')) call('server', '${server.name}', 'forceStopServer');" <#else>disabled</#if> class="btn btn-danger btn-xs">Kill</button>
                    </div>
                </td>
                <td onclick="window.document.location='/servers/${server.name}'">${server.motd}</td>
            </tr>
        </#if>
    </#list>
    </tbody>
</table>
<script>
    function openPopup($url) {
        window.open(window.location.origin + $url, '_new', 'height=500,width=800');
    }
</script>
<#include "footer.ftl">
