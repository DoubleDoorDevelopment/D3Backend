<#include "header.ftl">
<link href="/static/css/signin.css" rel="stylesheet">
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
        Permission lvl: ${user.group}
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
