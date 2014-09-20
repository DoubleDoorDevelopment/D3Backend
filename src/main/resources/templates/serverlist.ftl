<#include "header.ftl">
<h1>Server list</h1>
<table class="table table-hover">
    <thead>
    <tr>
        <th class="col-sm-2">Server Name</th>
        <th class="col-sm-2">Server Address</th>
        <th class="col-sm-1">Players</th>
        <th class="col-sm-7">MOTD</th>
    </tr>
    </thead>
    <tbody>
    <#list servers as server>
        <tr class="<#if server.online>success<#else>danger</#if>" style="cursor:pointer;" onclick="window.document.location='/servers/${server.name}'">
            <td>${server.name}</td>
            <td>${server.displayAddress}</td>
            <td>${server.onlinePlayers}/${server.slots}</td>
            <td>${server.motd}</td>
        </tr>
    </#list>
    </tbody>
</table>
<#include "footer.ftl">
