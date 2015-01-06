<#include "header.ftl">
<h1>User list</h1>
<table class="table table-hover">
    <thead>
    <tr>
        <th class="col-sm-2">Username</th>
        <th class="col-sm-2">Group</th>
    <#if user.isAdmin()>
        <th class="col-sm-1">Max Servers</th>
        <th class="col-sm-1">Max RAM</th>
        <th class="col-sm-1">Max Diskspace</th>
        <th class="col-sm-2"></th>
    </#if>
    </tr>
    </thead>
    <tbody>
    <#list Settings.users as user1>
    <tr>
        <td>${user1.username}</td>
        <#if user.isAdmin()>
            <td><select class="form-control" onchange="call('users/${user1.username}', 'setGroup', [this.value])">
                <option>NORMAL</option>
                <option <#if user1.isAdmin()>selected="selected"</#if>>ADMIN</option>
            </select>
            </td>
            <td><input type="number" min="-1" class="form-control" placeholder="-1 is infinite" value="${user1.maxServers?c}" onchange="call('users/${user1.username}', 'setMaxServers', [this.value])"></td>
            <td><input type="number" min="-1" class="form-control" placeholder="-1 is infinite" value="${user1.maxRam?c}" onchange="call('users/${user1.username}', 'setMaxRam', [this.value])"></td>
            <td><input type="number" min="-1" class="form-control" placeholder="-1 is infinite" value="${user1.maxDiskspace?c}" onchange="call('users/${user1.username}', 'setMaxDiskspace', [this.value])"></td>
            <td>
                <div class="btn-group">
                    <button class="btn btn-warning" type="button" onclick="if (confirm('Are you sure?')) {call('users/${user1.username}', 'setPass', [makeid()]); }">Reset password</button>
                    <button class="btn btn-danger" type="button" onclick="if (confirm('Are you sure?')) {call('users/${user1.username}', 'delete'); }">Delete</button>
                </div>
            </td>
        <#else>
            <td>${user1.group}</td>
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

        for (var i = 0; i < 25; i++)
        {
            text += possible.charAt(Math.floor(Math.random() * possible.length));
        }

        prompt("Copy the next line, it is the new password.\nDon't bother changing it, that won't affect anything.", text);

        return text;
    }
</script>
<#include "footer.ftl">
