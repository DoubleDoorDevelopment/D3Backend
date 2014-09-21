<#include "header.ftl">
<#if !user??>
    <link href="/static/css/signin.css" rel="stylesheet">
    <form class="form-signin" role="form" method="post">
        <#if message??>
            <div class="alert alert-danger" role="alert">${message}</div>
        </#if>
        <h2 class="form-signin-heading">Make account</h2>
        <input name="username" type="text" class="form-control" placeholder="Username" required autofocus style="margin-bottom: -1px; border-bottom-right-radius: 0; border-bottom-left-radius: 0;">
        <input name="password" type="password" class="form-control" placeholder="Password" required style="margin-bottom: 10px; border-top-left-radius: 0; border-top-right-radius: 0;">
        <input name="areyouhuman" type="text" class="form-control" placeholder="2 + 2 = ?" required>
        <br>
        <button class="btn btn-lg btn-primary btn-block" type="submit">Register!</button>
    </form>
<#else>
    <h1>Account registered!</h1>
</#if>
<#include "footer.ftl">
