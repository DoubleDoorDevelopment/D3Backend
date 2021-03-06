<#include "header.ftl">
<link href="/static/css/signin.css" rel="stylesheet">
<#if !Helper.usingHttps()>
<div id="httpsWarning" class="alert alert-danger" role="alert"><b>This server is not using HTTPS</b>, your password will be send over the network in plaintext!</div>
<script>
    var httpsWarning = get("httpsWarning");
    if (location.protocol == 'https:' && httpsWarning != null)
    {
        httpsWarning.setAttribute("hidden", "hidden")
    }
</script>
</#if>
<#if !user??>
<form class="form-signin" role="form" method="post">
    <#if message??>
        <div class="alert alert-danger" role="alert">${message}</div>
    </#if>
    <h2 class="form-signin-heading">Please sign in</h2>
    <input name="username" type="text" class="form-control" placeholder="Username" required autofocus
           style="margin-bottom: -1px; border-bottom-right-radius: 0; border-bottom-left-radius: 0;">
    <input name="password" type="password" class="form-control" placeholder="Password" required
           style="margin-bottom: 10px; border-top-left-radius: 0; border-top-right-radius: 0;">
    <button class="btn btn-lg btn-primary btn-block" type="submit">Sign in!</button>
    <a href="/register" class="btn btn-lg btn-success btn-block" type="submit">Register</a>
</form>
<#else>
<div class="form-signin">
    <#if message??>
        <div class="alert alert-danger" role="alert">${message}</div>
    </#if>
    <h2 class="form-signin-heading">You are logged in!</h2>

    <p>
        Permission lvl: ${user.group}<br>
        Diskspace left: ${(user.diskspaceLeft == -1)?string("&infin;", user.diskspaceLeft)} MB<br>
        Servers left: ${(user.maxServers == -1)?string("&infin;", (user.maxServers - user.serverCount))}<br>
        RAM left: ${(user.maxRamLeft == -1)?string("&infin;", user.maxRamLeft)} MB<br>
    </p>
</div>
<form class="form-signin" role="form" method="post">
    <button name="logout" value="true" class="btn btn-lg btn-warning btn-block">Log out!</button>
</form>
<form class="form-signin" role="form" method="post">
    <input name="oldPassword" type="password" class="form-control" placeholder="Old Password" required
           style="margin-bottom: -1px; border-bottom-right-radius: 0; border-bottom-left-radius: 0;">
    <input name="newPassword" type="password" class="form-control" placeholder="New Password" required
           style="margin-bottom: 10px; border-top-left-radius: 0; border-top-right-radius: 0;">
    <button class="btn btn-lg btn-danger btn-block" type="submit">Change password</button>
</form>
</#if>
<#include "footer.ftl">
