<#include "header.ftl">
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
<link href="/static/css/signin.css" rel="stylesheet">
<form class="form-signin" role="form" method="post">
    <#if message??>
        <div class="alert alert-danger" role="alert">${message}</div>
    </#if>
    <h2 class="form-signin-heading">Make account</h2>
    <div id="name-div" class="form-group">
        <input name="username" type="text" class="form-control" placeholder="Username" required autofocus onchange="checkName()" id="username">
        <span class="help-block">Alphanumerical, max 15 characters.</span>
    </div>
    <div class="form-group">
        <input name="password" type="password" class="form-control" placeholder="Password" required>
    </div>
    <div class="form-group">
        <input name="areyouhuman" type="text" class="form-control" placeholder="2 + 2 = ?" required>
    </div>
    <div class="form-group">
        <button class="btn btn-lg btn-primary btn-block" type="submit" id="submit">Register!</button>
    </div>
</form>
<script>
    function checkName()
    {
        var name = get("username").value;

        if (name == null || !name.match(/^[0-9A-Za-z]+$/) || name.length > 16)
        {
            get("submit").disabled = true;
            get("name-div").className = "form-group has-error";
        }
        else
        {
            get("submit").disabled = false;
            get("name-div").className = "form-group";
        }
    }
</script>
<#else>
<h1>Account registered!</h1>
</#if>
<#include "footer.ftl">
