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
        </#if>
    </tr>
    </thead>
    <tbody>
    <#list users as user>
    <tr>
        <td>${user.username}</td>
        <#if admin>
            <td><select class="form-control" onchange="call('users', '${user.username}', 'setGroup', this.value)">
                <option>NORMAL</option>
                <option <#if user.group == "ADMIN">selected="selected"</#if>>ADMIN</option>
            </select>
            </td>
            <td><input type="number" min="-1" class="form-control" placeholder="-1 is infinite" value="${user.maxServers}" onchange="call('users', '${user.username}', 'setMaxServers', this.value)"></td>
            <td><input type="number" min="0" class="form-control" placeholder="0 is infinite" value="${user.maxRam}" onchange="call('users', '${user.username}', 'setMaxRam', this.value)"></td>
        <#else>
            <td>${user.group}</td>
        </#if>
    </tr>
    </#list>
    </tbody>
</table>
<#include "footer.ftl">
