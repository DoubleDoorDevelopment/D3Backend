<#include "header.ftl">
<h1>Server list</h1>
<table class="table table-hover tablesorter" id="servers" style="table-layout: fixed">
    <thead>
    <tr>
        <th class="col-sm-4">Server Name</th>
        <th class="col-sm-2">Server Address</th>
        <th class="col-sm-1">Players</th>
        <th class="col-sm-1">Total Size</th>
        <th class="col-sm-2" style="min-width: 174px;"></th>
        <th class="col-sm-2">RAM (Min Max)</th>
    </tr>
    </thead>
</table>
<script src="/static/js/jquery.dataTables.min.js"></script>
<script>
    function createBtnGroup(row, type, set1, meta)
    {
        return '<div class="btn-group">' +
                '<button type="button" ' + (!row.online ? 'onclick="call(\'servercmd/' + row.id + '\', \'startServer\')"' : 'disabled') + ' class="btn btn-success btn-xs">Start</button>' +
                '<button type="button" class="btn btn-info btn-xs" onclick="openPopup(\'/serverconsole?server=' + row.id + '\')">Console</button>' +
                '<button type="button" ' + (row.online ? 'onclick="call(\'servercmd/' + row.id + '\', \'stopServer\', [prompt(\'Message?\', \'Server is stopping.\')])"' : 'disabled') + ' class="btn btn-warning btn-xs">Stop</button>' +
                '<button type="button" ' + (row.online ? 'onclick="if (confirm(\'Are you sure?\')) call(\'servercmd/' + row.id + '\', \'forceStopServer\');"' : 'disabled') + ' class="btn btn-danger btn-xs">Kill</button>' +
                '</div>'
    }

    function makeClickable(td, cellData, rowData, row, col)
    {
        $(td).click(function ()
        {
            window.document.location = "/server?server=" + rowData.id
        });
    }

    var table = $('#servers').DataTable({
        paging: false,
        searching: false,
        data: [],
        createdRow: function (row, data, dataIndex)
        {
            var jcrow = $(row);
            jcrow.addClass(data.online ? 'success' : 'danger');
            jcrow.css("cursor", "pointer");
        },
        columns: [
            {data: 'id', createdCell: makeClickable},
            {data: 'displayAddress', createdCell: makeClickable},
            {data: function (row) {
                return row['onlinePlayers'] + " / " + row['slots']
            }, createdCell: makeClickable},
            {data: 'size', createdCell: makeClickable},
            {data: createBtnGroup},
            {data: 'ram', createdCell: makeClickable},
        ]
    });
    var websocket = new WebSocket(wsurl("serverlist"));
    websocket.onmessage = function (evt)
    {
        var temp = JSON.parse(evt.data);
        if (temp.status === "ok")
        {
            table.rows(function (idx, data, node)
            {
                return data.id === temp.data.id;
            }).remove();
            if (!temp.data.hasOwnProperty("deleted") || !temp.data['deleted'])
            {
                table.row.add(temp.data);
            }
            table.draw();
        }
        else
        {
            addAlert(temp.message);
        }
    };
    websocket.onerror = function (evt)
    {
        addAlert("The websocket errored. Refresh the page!")
    };
    websocket.onclose = function (evt)
    {
        addAlert("The websocket closed. Refresh the page!")
    }
</script>
<#include "footer.ftl">
