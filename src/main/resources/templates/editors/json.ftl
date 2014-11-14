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
    <button type="button" id="savebtn" class="btn btn-primary btn-block" onclick="callNoRefresh('filemanager', '${fm.server.ID}', '${fm.stripServer(fm.file)}', 'set', JSON.stringify(editor.get()));">Save</button>
    <#else>
    <p>File is readonly.</p>
    </#if>
    <script type="text/javascript">
        var container = document.getElementById('jsoneditor');

        var options = {
            mode: 'code',
            modes: ['code', 'tree'], // allowed modes
            error: function (err) {
                alert(err.toString());
            }
        };

        var json = ${fm.getFileContents()!"null"};

        if (json == null) {
            alert("Data file might be currupt. It can't be read by our parser.");
            document.getElementById("savebtn").disabled = true;
            document.getElementById("jsoneditor").innerHTML = "Error. File corrupt?";
        }
        else editor = new JSONEditor(container, options, json);
    </script>
</div>