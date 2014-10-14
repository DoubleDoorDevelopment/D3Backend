<#include "header.ftl">
<#assign allowModify = server.canUserControl(user) >
<#assign isCoOwner = server.isCoOwner(user) >
${wm.update()}
<h1>World Manager <small> ${server.name}   <span class="label label-<#if server.online>success<#else>danger</#if>"><#if server.online>Online<#else>Offline</#if></span></small></h1>
<div class="panel panel-info">
    <div class="panel-heading">
        <h3 class="panel-title" style="text-align: center;">World information</h3>
        <span class="pull-right clickable"><i class="fa fa-chevron-up"></i></span>
    </div>
    <div class="panel-body" style="text-align: center;">
        <p>
            World folder: ${wm.worldName}.<br>
        </p>
        <p class="text-primary">
            Diskspace in use:<br>
            by server: ${server.diskspaceUse[0]}MB<br>
            by backups: ${server.diskspaceUse[1]}MB<br>
            total: ${server.diskspaceUse[2]}MB<br>
        </p>
        <p>
            Make a backup now:
        </p>
        <div class="btn-group">
            <button type="button" <#if isCoOwner>onclick="call('worldmanager', '${server.name}', 'makeWorldBackup')" <#else>disabled</#if> class="btn btn-info">The World</button>
            <button type="button" <#if isCoOwner>onclick="call('worldmanager', '${server.name}', 'makeAllOfTheBackup')" <#else>disabled</#if> class="btn btn-info">EVERYTHING</button>
        </div>
    </div>
</div>
<div class="row">
    <#list wm.dimentionMap?values as dim>
        <div class="col-sm-4">
            <div class="panel panel-default">
                <div class="panel-heading">
                    <h3 class="panel-title" style="text-align: center;">DIM${dim.dimid}</h3>
                    <span class="pull-right clickable"><i class="fa fa-chevron-up"></i></span>
                </div>
                <div class="panel-body" style="text-align: center;">
                    <button type="button" <#if isCoOwner>onclick="call('worldmanager_dim', '${server.name}', '${dim.dimid}', 'makeBackup')" <#else>disabled</#if> class="btn btn-info">Backup</button>
                    <button type="button" onclick="if (confirm('Are you sure?\nThis will delete the entire dimention!')) call('worldmanager_dim', '${server.name}', '${dim.dimid}', 'delete');" class="btn btn-danger">Delete</button>
                </div>
            </div>
        </div>
    </#list>
</div>
<style>
    .panel-heading span {
        margin-top: -20px;
        font-size: 15px;
    }
    .row {
        margin-top: 40px;
        padding: 0 10px;
    }
    .clickable {
        cursor: pointer;
    }
</style>
<#include "footer.ftl">
