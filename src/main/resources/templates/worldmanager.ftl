<#include "header.ftl">
<#assign wm = server.getWorldManager()>
<#assign allowModify = server.canUserControl(user) >
<#assign isCoOwner = server.isCoOwner(user) >
<#-- This needs to happen BEFORE anything loads off the template! -->${wm.update()}
<h1>World Manager
    <small><a href="/server?server=${server.ID?js_string}">${server.ID?js_string}</a> <span id="online"></span></small>
</h1>
<!-- TODO
<p>
    <b>None of the buttons on this page work as the socket connection handler hasn't been ported to the new version.</b><br>
    Also, extra functionality is to be added.
</p> -->
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
            by server: ${server.diskspaceUse[0]} MB<br>
            by backups: ${server.diskspaceUse[1]} MB<br>
            total: ${server.diskspaceUse[2]} MB<br>
            Diskspace left: ${(user.diskspaceLeft == -1)?string("&infin;", user.diskspaceLeft)} MB
        </p>

        <p>
            Make a backup now:
        </p>

        <div class="btn-group">
            <button type="button" <#if isCoOwner>onclick="call('worldmanager/${server.ID?js_string}', 'makeWorldBackup', [], progressModal)" <#else>disabled</#if> class="btn btn-info">The World</button>
            <button type="button" <#if isCoOwner>onclick="call('worldmanager/${server.ID?js_string}', 'makeAllOfTheBackup', [], progressModal)" <#else>disabled</#if> class="btn btn-info">EVERYTHING</button>
        </div>
    </div>
</div>
<div class="row">
<#list server.getDimensionMap()?keys as dimid>
    <div class="col-sm-4">
        <div class="panel panel-default">
            <div class="panel-heading">
                <h3 class="panel-title" style="text-align: center;">${dimid}</h3>
                <span class="pull-right clickable"><i class="fa fa-chevron-up"></i></span>
            </div>
            <div class="panel-body" style="text-align: center;">
                <button type="button" <#if isCoOwner>onclick="call('worldmanager/${server.ID?js_string}', 'makeBackup', ['${dimid}'], progressModal)" <#else>disabled</#if> class="btn btn-info">Backup</button>
                <button type="button" <#if isCoOwner>onclick="if (confirm('Are you sure?\nThis will delete the entire dimention!')) call('worldmanager/${server.ID?js_string}', 'delete', ['${dimid}']);" <#else>disabled</#if> class="btn btn-danger">
                    Delete
                </button>
            </div>
        </div>
    </div>
</#list>
</div>
<!-- Modal -->
<div class="modal fade" id="modal" tabindex="-1" role="dialog" aria-labelledby="modalLabel" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">&times;</span><span class="sr-only">Close</span></button>
                <h4 class="modal-title" id="modalLabel">Operation in progress</h4>
            </div>
            <div class="modal-body" id="modal-body">
                <pre id="modal-log"></pre>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
            </div>
        </div>
    </div>
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

    #modal-log {
        overflow: auto;
        max-height: 60vh;
        word-wrap: normal;
        white-space: pre;
    }
</style>
<script>
    var modal = $('#modal');
    var needsShowing = true;
    function progressModal(data)
    {
        if (needsShowing)
        {
            modal.modal("show");
            document.getElementById("modal-log").innerHTML = "";
            needsShowing = false;
        }
        if (data === "done")
        {
            needsShowing = true;
            document.getElementById("modal-log").innerHTML += "-- ALL DONE --\n";
        }
        document.getElementById("modal-log").innerHTML += data + "\n";
    }
</script>
<#include "footer.ftl">
