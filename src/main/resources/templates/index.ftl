<#include "header.ftl">
<#if !Helper.usingHttps()>
<div id="httpsWarning" class="alert alert-danger" role="alert"><b>This server is not using HTTPS</b>, your password will be send over the network in plaintext!</div></#if>
<h1>Home</h1>
<h3>Some statistics</h3>
<p>
    Servers made: ${Settings.servers?size}<br>
    Servers online: ${Settings.onlineServers?size}<br>
    Total RAM usage: ${Helper.getTotalRamUsed() / 1024} GB<br>
    Users: ${Settings.users?size}<br>
    Diskspace used: ${Helper.getTotalDiskspaceUsed() / 1024} GB<br>
    Players online on all servers: ${Helper.getGlobalPlayers()}<br>
    Backend has been online for: ${Helper.getOnlineTime("%2d days, ", "%2d hours, ", "%2d min and ", "%2d sec")}<br>
</p>
<script>
    var httpsWarning = document.getElementById("httpsWarning");
    if (location.protocol == 'https:' && httpsWarning != null)
    {
        httpsWarning.setAttribute("hidden", "hidden")
    }
</script>
<#include "footer.ftl">
