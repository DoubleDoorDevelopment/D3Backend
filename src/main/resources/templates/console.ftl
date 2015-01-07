<#include "header.ftl">
<textarea class="textarea form-control" id="text" style="height: 800px;" wrap="off"></textarea>
<p>Ps: You can't give any input from this page. You need SSH access for that.</p>
<script>
    function wsurl(s)
    {
        var l = window.location;
        return (l.protocol === "https:" ? "wss://" : "ws://") + l.hostname + ":" + l.port + "/socket/" + s;
    }
    var autoScroll = true;
    var textarea = document.getElementById('text');
    var websocket = new WebSocket(wsurl("console"));
    websocket.onerror = function (evt)
    {
        alert("The websocket errored. Refresh the page!")
    };
    websocket.onclose = function (evt)
    {
        alert("The websocket closed. Refresh the page!")
    };

    function htmlDecode(input)
    {
        var e = document.createElement('div');
        e.innerHTML = input;
        return e.childNodes.length === 0 ? "" : e.childNodes[0].nodeValue;
    }

    websocket.onmessage = function (evt)
    {
        var temp = JSON.parse(evt.data);
        if (temp.status !== "ok")
        {
            alert(temp.message);
        }
        else
        {
            autoScroll = textarea.scrollHeight <= textarea.scrollTop + textarea.clientHeight + 50;
            var total = ((textarea.value ? textarea.value : "") + htmlDecode(temp.data)).split("\n");
            if (total.length > 1000) total = total.slice(total.length - 1000);
            textarea.value = total.join("\n");
            if (autoScroll) textarea.scrollTop = textarea.scrollHeight;
        }
    }
</script>
<#include "footer.ftl">