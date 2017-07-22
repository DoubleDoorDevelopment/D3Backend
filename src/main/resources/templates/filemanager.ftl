<#include "header.ftl">
<h1>File Manager
    <small><a href="/server?server=${server.ID?url}">${server.ID?url}</a> <span id="online"></span></small>
</h1>
<#if !fm.file.exists()>
<div class="panel panel-danger">
    <div class="panel-heading"><#list fm.makeBreadcrumbs() as file> / <a href="?server=${server.ID?url}&file=${fm.stripServer(file)?url}">${file.getName()?url}</a></#list></div>
    <div class="panel-body">
        <h4>File not found.</h4>
    </div>
</div>
<#elseif fm.file.isDirectory() >
<div class="panel panel-info">
    <div class="panel-heading"><#list fm.makeBreadcrumbs() as file> / <a href="?server=${server.ID?url}&file=${fm.stripServer(file)?url}">${file.getName()?url}</a></#list></div>
    <div class="panel-body">
        <form class="form-inline" enctype="multipart/form-data" method="post" id="uploadform">
            <div class="form-group">
                <input type="file" name="fileName" onchange="uploadFile()">
            </div>
            <div class="form-group btn-group">
                <button type="button" onclick="{var n = prompt('New file name?', ''); if (n != null) call('filemanager/${fm.server.ID?url}/${fm.stripServer(fm.file)?url}', 'newFile', [n]);}" class="btn btn-default btn-xs">New file</button>
                <button type="button" onclick="{var n = prompt('New folder name?', ''); if (n != null) call('filemanager/${fm.server.ID?url}/${fm.stripServer(fm.file)?url}', 'newFolder', [n]);}" class="btn btn-default btn-xs">New folder</button>
            </div>
        </form>
    </div>
    <table class="table table-hover table-condensed tablesorter" id="servers">

    </table>
</div>
<script src="/static/js/jquery.dataTables.min.js"></script>
<script>
    function encode(str)
    {
        return encodeURIComponent(str).replace(/[!'()*]/g, function(c)
        {
            return '%' + c.charCodeAt(0).toString(16);
        });
    }

    var table = $('#servers').DataTable({
        paging: false,
        order: [[ 1, 'asc' ]],
        data: ${fm.getJson(fm.file)},
        columns: [
            { title: "", render: function(data, type, row, meta)
                {
                    return '<i class="fa fa-' + row.icon + '"></i>';
                }
            },
            { title: "File name", "class": "col-sm-5", render: function(data, type, row, meta)
                {
                    if (row.canEdit)
                    {
                        var out = '<a href="?server=${server.ID?url}&file=' + encode(row.url) + '"';
                        if (row.hasOwnProperty("tooltip")) out += 'rel="tooltip" data-toggle="tooltip" data-placement="top" title="' + row.tooltip + '"';
                        out += '>' + row.name + '</a>';
                        return out;
                    }
                    else
                    {
                        return row.name;
                    }
                }
            },
            { title: "File size", "class": "col-sm-1", data: "fileSize"},
            { title: "Last modified", "class": "col-sm-2", data: "lastModified"},
            { title: "", "class": "col-sm-1", render: function(data, type, row, meta)
                {
                    if (!row.isFolder)
                    {
                        return '<a type="button" class="btn btn-default btn-xs" href="/raw/${server.ID?url}/' + encodeURI(row.url) + '">Raw file</a>';
                    }
                    return "";
                }
            },
            { title: "", "class": "col-sm-3", render: function(data, type, row, meta)
                {
                    if (row.canWrite)
                    {
                        var out = '<div class="btn-group">';
                        out += '<button type="button" onclick="rename(\'' + encode(row.name) + '\', \'' + encode(row.url) + '\')" class="btn btn-default btn-xs">Rename</button>';
                        out += '<button type="button" onclick="del(\'' + encode(row.url) + '\')" class="btn btn-danger btn-xs">Delete</button>';

                        if (row.extension === "jar" || row.extension === "zip")
                        {
                            out += '<button type="button" onclick="call(\'filemanager/${fm.server.ID?url}/' + encode(row.url) + '\', \'rename\', [\'' + encode(row.name) + '.disabled\']);" class="btn btn-default btn-xs">Disable</button>';
                        }
                        else if (row.extension === "disabled")
                        {
                            out += '<button type="button" onclick="call(\'filemanager/${fm.server.ID?url}/' + encode(row.url) + '\', \'rename\', [\'' + encode(row.name.replace(".disabled", "")) + '\']);" class="btn btn-default btn-xs">Enable</button>';
                        }

                        out += '</div>';
                        return out;
                    }
                    else
                    {
                        return '<button type="button" onclick="call(\'filemanager/${fm.server.ID?url}/' + encode(row.url) + '\', \'makeWritable\');" class="btn btn-info btn-xs">Make writeable</button>';
                    }
                }
            }
        ]
    });

    websocketMonitor = new WebSocket(wsurl("filemonitor/${server.ID?url}/${fm.stripServer(fm.file)?url}"));
    websocketMonitor.onerror = function (evt)
    {
        addAlert("The websocket errored. Refresh the page!")
    };
    websocketMonitor.onclose = function (evt)
    {
        addAlert("The websocket closed. Refresh the page!")
    };
    websocketMonitor.onmessage = function (evt)
    {
        var temp = JSON.parse(evt.data);
        if (temp.status === "ok")
        {
            table.clear().rows.add(temp.data).draw();
        }
        else
        {
            addAlert(temp.message);
        }
    };

    function uploadFile()
    {
        $('#uploadform').submit();
    }

    function del(urlpart)
    {
        if (confirm('Are you sure?\nIt will be gone FOREVER!'))
        {
            call('filemanager/${fm.server.ID?url}/' + urlpart, 'delete', []);
        }
    }

    function rename(oldname, urlpart)
    {
        var n = prompt('New file name?', decodeURIComponent(oldname));
        if (n != null)
        {
            call('filemanager/${fm.server.ID?url}/' + urlpart, 'rename', [n]);
        }
    }
</script>
<#else >
    <#assign readonly = !fm.file.canWrite()>
<div class="panel panel-<#if readonly>warning<#elseif fm.getEditor()??>success<#else>danger</#if>">
    <div class="panel-heading"><#list fm.makeBreadcrumbs() as file> /
        <a href="?server=${server.ID?url}&file=${fm.stripServer(file)?url}" <#if file.getName()?ends_with(".dat") && Helper.getUsernameFromUUID(file.getName())??>rel="tooltip" data-toggle="tooltip" data-placement="top" title="${Helper.getUsernameFromUUID(file.getName())}"</#if>>${file.getName()}</a></#list>
    </div>
    <#if fm.getEditor()??>
        <#include "editors/" + fm.getEditor()>
    <#else>
        <div class="panel-body">
            <h4>This kind of file can't be displayed.</h4>
        </div>
    </#if>
</div>
</#if>
<script>
    $('[rel=tooltip]').tooltip()
</script>
<#include "footer.ftl">
