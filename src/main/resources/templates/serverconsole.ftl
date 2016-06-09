<!DOCTYPE html>
<!--
  ~     D3Backend
  ~     Copyright (C) 2015  Dries007 & Double Door Development
  ~
  ~     This program is free software: you can redistribute it and/or modify
  ~     it under the terms of the GNU Affero General Public License as published
  ~     by the Free Software Foundation, either version 3 of the License, or
  ~     (at your option) any later version.
  ~
  ~     This program is distributed in the hope that it will be useful,
  ~     but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~     GNU Affero General Public License for more details.
  ~
  ~     You should have received a copy of the GNU Affero General Public License
  ~     along with this program.  If not, see <http://www.gnu.org/licenses/>.
  -->
<html>
<head lang="en">
    <meta charset="UTF-8">
    <title>Console ${server.ID?js_string}</title>
    <!-- Le meta -->
    <meta name="author" content="Dries007">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <!-- Le styles -->
    <link href="/static/css/bootstrap.min.css" rel="stylesheet">
    <link href="/static/css/font-awesome.min.css" rel="stylesheet">
    <link rel="shortcut icon" type="image/ico" href="/static/favicon.ico"/>
</head>
<body onresize="document.getElementById('text').style.height = (window.innerHeight - 35) + 'px';">
<textarea class="textarea form-control" id="text" style="height: 465px;"></textarea>
<input type="text" class="form-control" placeholder="Command..." onkeydown="if (event.keyCode == 13) {send(this.value); this.value = ''}">
<script>
    function wsurl(s)
    {
        var l = window.location;
        return (l.protocol === "https:" ? "wss://" : "ws://") + l.hostname + ":" + l.port + "/socket/" + s;
    }
    var textarea = document.getElementById("text");
    var autoScroll = true;
    var websocket = new WebSocket(wsurl("serverconsole/${server.ID?js_string}"));
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
        if (temp.status !== "ok")
        {
            alert(temp.message);
        }
        else
        {
            autoScroll = textarea.scrollHeight <= textarea.scrollTop + textarea.clientHeight + 50;
            var total = ((textarea.value ? textarea.value + "\n" : "") + temp.data).split("\n");
            if (total.length > 1000) total = total.slice(total.length - 1000);
            textarea.value = total.join("\n");
            if (autoScroll) textarea.scrollTop = textarea.scrollHeight;
        }
    };

    function send(data)
    {
        websocket.send(data);
    }
</script>
</body>
</html>