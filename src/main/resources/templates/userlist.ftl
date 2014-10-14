<#include "header.ftl">
<h1>User list</h1>
<table class="table table-hover">
    <thead>
    <tr>
        <th class="col-sm-2">Username</th>
        <th class="col-sm-2">Group</th>
    <#if admin>
        <th class="col-sm-1">Max Servers</th>
        <th class="col-sm-1">Max RAM</th>
        <th class="col-sm-1">Max Diskspace</th>
        <th class="col-sm-2"></th>
    </#if>
    </tr>
    </thead>
    <tbody>
    <#list Settings.users as user>
    <tr>
        <td>${user.username}</td>
        <#if admin>
            <td><select class="form-control" onchange="call('users', '${user.username}', 'setGroup', this.value)">
                <option>NORMAL</option>
                <option <#if user.isAdmin()>selected="selected"</#if>>ADMIN</option>
            </select>
            </td>
            <td><input type="number" min="-1" class="form-control" placeholder="-1 is infinite" value="${user.maxServers?c}" onchange="call('users', '${user.username}', 'setMaxServers', this.value)"></td>
            <td><input type="number" min="-1" class="form-control" placeholder="-1 is infinite" value="${user.maxRam?c}" onchange="call('users', '${user.username}', 'setMaxRam', this.value)"></td>
            <td><input type="number" min="-1" class="form-control" placeholder="-1 is infinite" value="${user.maxDiskspace?c}" onchange="call('users', '${user.username}', 'setMaxDiskspace', this.value)"></td>
            <td>
                <div class="btn-group">
                    <button class="btn btn-warning" type="button" onclick="if (confirm('Are you sure?')) {call('users', '${user.username}', 'setPass', makeid()); }">Reset password</button>
                    <button class="btn btn-danger" type="button" onclick="if (confirm('Are you sure?')) {call('users', '${user.username}', 'delete'); }">Delete</button>
                </div>
            </td>
        <#else>
            <td>${user.group}</td>
        </#if>
    </tr>
    </#list>
    </tbody>
</table>
<script>
    function makeid()
    {
        var text = "";
        var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        for( var i=0; i < 25; i++ )
            text += possible.charAt(Math.floor(Math.random() * possible.length));

        prompt("Copy the next line, it is the new password.\nDon't bother changing it, that won't affect anything.", text)

        return text;
    }
</script>
<#include "footer.ftl">
