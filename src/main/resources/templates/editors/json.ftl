<div class="panel-body">
    <!-- This page uses "json editor V 3.1.2", inspired by http://jsoneditoronline.org, source from https://github.com/josdejong/jsoneditor -->
    <!-- json editor -->
    <link rel="stylesheet" type="text/css" href="/static/css/jsoneditor.min.css">
    <script type="text/javascript" src="/static/js/jsoneditor.min.js"></script>

    <!-- ace editor -->
    <script type="text/javascript" src="/static/js/ace/ace.js"></script>

    <!-- json lint -->
    <script type="text/javascript" src="/static/js/jsonlint.js"></script>

    <style type="text/css">
        #jsoneditor {
            width: 100%;
            height: 500px;
        }
    </style>
    <div id="jsoneditor"></div>
<#if !readonly>
    <button type="button" id="savebtn" class="btn btn-primary btn-block" onclick="send()">Save</button>
<#else>
    <p>File is readonly.</p>
</#if>
    <script type="text/javascript">
        var container = document.getElementById('jsoneditor');

        var options = {
            mode: 'code',
            modes: ['code', 'tree'], // allowed modes
            error: function (err)
            {
                alert(err.toString());
            }
        };

        var json = ${fm.getFileContents()!"null"};

        if (json == null)
        {
            alert("Data file might be currupt. It can't be read by our parser.");
            document.getElementById("savebtn").disabled = true;
            document.getElementById("jsoneditor").innerHTML = "Error. File corrupt?";
        }
        else
        {
            editor = new JSONEditor(container, options, json);
        }

        var websocket = new WebSocket(wsurl("filemanager/${server.ID}/${fm.stripServer(fm.getFile())}"));
        websocket.onerror = function (evt)
        {
            alert("The websocket errored. Refresh the page!")
        };
        websocket.onclose = function (evt)
        {
            alert("The websocket closed. Refresh the page!")
        };
        function send()
        {
            websocket.send(websocket.send(JSON.stringify({method: "set", args: [JSON.stringify(editor.get())]}));
        )
        }
        websocket.onmessage = function (evt)
        {
            var temp = JSON.parse(evt.data);
            if (temp.status === "ok")
            {
                editor.set(JSON.parse(temp.data));
            }
            else
            {
                alert(temp.message);
            }
        }
    </script>
</div>