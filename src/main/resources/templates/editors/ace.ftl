<style type="text/css" media="screen">
    #editor {
        width: 100%;
        height: 500px;
    }
</style>

<div id="editor">${fm.getFileContentsAsString()}</div>

<script src="/static/js/ace/ace.js" type="text/javascript" charset="utf-8"></script>
<script>
    var editor = ace.edit("editor");
    editor.setTheme("ace/theme/chrome");
    editor.getSession().setMode("ace/mode/html");
    editor.getSession().setTabSize(4);
    editor.getSession().setUseSoftTabs(true);
    editor.setShowPrintMargin(false);

</script>
<#if !readonly>
<button type="button" class="btn btn-primary btn-block" onclick="call('filemanager', '${fm.server.name}', '${fm.stripServer(fm.file)}', 'set', editor.getValue());">Save</button>
<#else>
<p>File is readonly.</p>
</#if>