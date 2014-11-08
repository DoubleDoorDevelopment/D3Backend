<#include "header.ftl">
<h1>Server list</h1>
<table class="table table-hover tablesorter" id="servers">
    <thead>
    <tr>
        <th class="col-sm-2">Server Name</th>
        <th class="col-sm-2">Server Address</th>
        <th class="col-sm-1">Players</th>
        <th class="col-sm-1">Total Size</th>
        <th class="col-sm-2"></th>
        <th class="col-sm-4">MOTD</th>
    </tr>
    </thead>
    <#if false>
        <tbody>
        <#list Settings.servers as server>
            <#if server.canUserControl(user)>
            <tr class="<#if server.online>success<#else>danger</#if>" style="cursor:pointer;">
                <td onclick="link('${server.ID}')">${server.ID}</td>
                <td onclick="link('${server.ID}')">${server.displayAddress}</td>
                <td onclick="link('${server.ID}')">${server.onlinePlayers}/${server.slots}</td>
                <td onclick="link('${server.ID}')">${server.diskspaceUse[2]} MB</td>
                <td>
                    <div class="btn-group">
                        <button type="button" <#if !server.online>onclick="call('server', '${server.ID}|startServer', location.reload())" <#else>disabled</#if> class="btn btn-success btn-xs">Start</button>
                        <button type="button" class="btn btn-info btn-xs" onclick="openPopup('/console?server=${server.ID}')">Console</button>
                        <button type="button" <#if server.online>onclick="call('server', '${server.ID}|stopServer|' + prompt('Message?', 'Server is stopping.'))" <#else>disabled</#if> class="btn btn-warning btn-xs">Stop</button>
                        <button type="button" <#if server.online>onclick="if (confirm('Are you sure?')) call('server', '${server.ID}|forceStopServer');" <#else>disabled</#if> class="btn btn-danger btn-xs">Kill</button>
                    </div>
                </td>
                <td onclick="link('${server.ID}')">${server.motd}</td>
            </tr>
            </#if>
        </#list>
        </tbody>
    </#if>
</table>
<script src="/static/js/jquery.dataTables.min.js"></script>
<script>
    function createBtnGroup(row, type, set1, meta)
    {
        return '<div class="btn-group">' +
            '<button type="button" ' + (!row.online ? 'onclick="call(\'server\', \'' + row.id + '\'|startServer\')")' : 'disabled') + ' class="btn btn-success btn-xs">Start</button>' +
            '<button type="button" class="btn btn-info btn-xs" onclick="openPopup(\'/console?server=' + row.id + '\')">Console</button>' +
            '<button type="button" ' + (!row.online ? 'onclick="call(\'server\', \'' + row.id + '\'|stopServer|\' + prompt(\'Message?\', \'Server is stopping.\')\')")' : 'disabled') + ' class="btn btn-warning btn-xs">Stop</button>' +
            '<button type="button" ' + (!row.online ? 'onclick="if (confirm(\'Are you sure?\')) call(\'server\', \'' + row.id +'\'|forceStopServer\');"' : 'disabled') + ' class="btn btn-danger btn-xs">Kill</button>' +
        '</div>'
    }

    function makeClickable(td, cellData, rowData, row, col)
    {
        $(td).click(function() {window.document.location = "/server?server=" + rowData.id});
    }

    table = $('#servers').DataTable({
        paging: false,
        searching: false,
        data: [],
        createdRow: function( row, data, dataIndex )
        {
            var jcrow = $(row);
            jcrow.addClass( data.online ? 'success' : 'danger');
            jcrow.css("cursor", "pointer");
        },
        columns: [
            { data: 'id', createdCell:makeClickable },
            { data: 'displayAddress', createdCell:makeClickable },
            { data: 'onlinePlayers', createdCell:makeClickable },
            { data: 'size', createdCell:makeClickable },
            { data: createBtnGroup },
            { data: 'motd', createdCell:makeClickable }
        ]
    });
    websocket = new WebSocket(wsurl("serverlist"));
    websocket.onmessage = function (evt)
    {
        var temp = JSON.parse(evt.data);
        if (temp.status === "ok")
        {

            table.rows(function (idx, data, node)
            {
                return data.id === temp.data.id;
            }).remove();
            table.row.add(temp.data);

            table.draw();
        }
        else alert(temp.message);
    }
    websocket.onerror =  function (evt) { alert("The websocket errored. Refresh the page!") }
    websocket.onclose =  function (evt) { alert("The websocket closed. Refresh the page!") }
</script>
<#include "footer.ftl">
