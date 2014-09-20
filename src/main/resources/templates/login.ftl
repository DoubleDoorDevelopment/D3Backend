<#include "header.ftl">
<#if !user??>
    <link href="/static/css/signin.css" rel="stylesheet">
    <form class="form-signin" role="form" method="post">
        <h2 class="form-signin-heading">Please sign in</h2>
        <input type="text" class="form-control" placeholder="Username" required autofocus>
        <input type="password" class="form-control" placeholder="Password" required>
        <button class="btn btn-lg btn-primary btn-block" type="submit">Sign in</button>
    </form>
<#else>
    <h1>Log in!</h1>
</#if>
<#include "footer.ftl">
