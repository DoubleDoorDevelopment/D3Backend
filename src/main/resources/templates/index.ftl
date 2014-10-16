<#include "header.ftl">
<#if !Helper.usingHttps()><div class="alert alert-danger" role="alert"><b>This server is not using HTTPS</b>, your password will be send over the network in plaintext!</div></#if>
<h1>Home</h1>
<h3>Some statistics</h3>
<p>
    Servers made: ${Settings.servers?size}<br>
    Servers online: ${Settings.onlineServers?size}<br>
    Total RAM usage: ${Helper.getTotalRamUsed() / 1024} GB<br>
    Users: ${Settings.users?size}<br>
    Diskspace used: ${Helper.getTotalDiskspaceUsed() / 1024} GB
</p>
<#include "footer.ftl">
