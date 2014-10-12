<#include "header.ftl">
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
