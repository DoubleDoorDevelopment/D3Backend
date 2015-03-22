<div class="panel-body">
    <style type="text/css" media="screen">
        #editor {
            width: 100%;
            height: 500px;
        }
    </style>

    <select id="modeSelector" onchange="setMode(this.value)">
    </select>

    <div id="editor">${fm.getFileContents()}</div>

    <script src="/static/js/ace/ace.js" type="text/javascript" charset="utf-8"></script>
    <script>
        var editor = ace.edit("editor");
        editor.setTheme("ace/theme/chrome");
        editor.getSession().setTabSize(4);
        editor.getSession().setUseSoftTabs(true);
        editor.setShowPrintMargin(false);

        var modes = ["abap", "actionscript", "ada", "apache_conf", "applescript", "asciidoc", "assembly_x86", "autohotkey", "batchfile", "c9search", "cirru", "clojure", "cobol", "coffee", "coldfusion", "csharp", "css", "curly", "c_cpp", "d", "dart", "diff", "django", "dockerfile", "dot", "eiffel", "ejs", "erlang", "forth", "ftl", "gcode", "gherkin", "gitignore", "glsl", "golang", "groovy", "haml", "handlebars", "haskell", "haxe", "html", "html_ruby", "ini", "io", "jack", "jade", "java", "javascript", "json", "jsoniq", "jsp", "jsx", "julia", "latex", "less", "liquid", "lisp", "livescript", "logiql", "lsl", "lua", "luapage", "lucene", "makefile", "markdown", "matlab", "mel", "mushcode", "mysql", "nix", "objectivec", "ocaml", "pascal", "perl", "pgsql", "php", "plain_text", "powershell", "praat", "prolog", "properties", "protobuf", "python", "r", "rdoc", "rhtml", "ruby", "rust", "sass", "scad", "scala", "scheme", "scss", "sh", "sjs", "smarty", "snippets", "soy_template", "space", "sql", "stylus", "svg", "tcl", "tex", "text", "textile", "toml", "twig", "typescript", "vala", "vbscript", "velocity", "verilog", "vhdl", "xml", "xquery", "yaml"];

        var modeSelector = document.getElementById("modeSelector");
        modes.forEach(function (mode)
        {
            modeSelector.innerHTML += "<option value=\"" + mode + "\">" + mode + "</option>";
        });

        var extention = "${fm.getExtension()}";
        if (modes.indexOf(extention) != -1)
        {
            modeSelector.value = extention;
            editor.getSession().setMode("ace/mode/" + extention);
        }
        else if ("${fm.stripServer(fm.file)}".indexOf("computer") != -1)
        {
            modeSelector.value = "lua";
            editor.getSession().setMode("ace/mode/lua");
        }
        else
        {
            modeSelector.value = "text";
            editor.getSession().setMode("ace/mode/text");
        }

        function setMode(mode)
        {
            editor.getSession().setMode("ace/mode/" + mode);
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
        websocket.onmessage = function (evt)
        {
            var temp = JSON.parse(evt.data);
            if (temp.status === "ok")
            {
                editor.setValue(temp.data);
                editor.clearSelection();
            }
            else
            {
                alert(temp.message);
            }
        };
        function send(data)
        {
            websocket.send(JSON.stringify({method: "set", args: [data]}));
        }
    </script>
<#if !readonly>
    <button type="button" class="btn btn-primary btn-block" onclick="send(editor.getValue());">Save</button>
<#else>
    <p>File is readonly.</p>
</#if>
</div>