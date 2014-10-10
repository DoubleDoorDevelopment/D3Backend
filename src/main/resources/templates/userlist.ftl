<#include "header.ftl">
<h1>User list</h1>
<table class="table table-hover">
    <thead>
    <tr>
        <th class="col-sm-2">Username</th>
        <th class="col-sm-2">Group</th>
    <#if admin>
        <th class="col-sm-2">Max Servers</th>
        <th class="col-sm-2">Max RAM</th>
        <th class="col-sm-2">Max Diskspace</th>
        <th class="col-sm-2">Max Backups</th>
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
                <option <#if user.group == "ADMIN">selected="selected"</#if>>ADMIN</option>
            </select>
            </td>
            <td><input type="number" min="-1" class="form-control" placeholder="-1 is infinite" value="${user.maxServers?c}" onchange="call('users', '${user.username}', 'setMaxServers', this.value)"></td>
            <td><input type="number" min="-1" class="form-control" placeholder="-1 is infinite" value="${user.maxRam?c}" onchange="call('users', '${user.username}', 'setMaxRam', this.value)"></td>
            <td><input type="number" min="-1" class="form-control" placeholder="-1 is infinite" value="${user.maxDiskspace?c}" onchange="call('users', '${user.username}', 'setMaxDiskspace', this.value)"></td>
            <td><input type="number" min="-1" class="form-control" placeholder="-1 is infinite" value="${user.maxBackups?c}" onchange="call('users', '${user.username}', 'setMaxBackups', this.value)"></td>
        <#else>
            <td>${user.group}</td>
        </#if>
    </tr>
    </#list>
    </tbody>
</table>
<#include "footer.ftl">
