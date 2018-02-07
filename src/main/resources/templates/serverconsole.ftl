<!DOCTYPE html>
<!--
  ~ D3Backend
  ~ Copyright (C) 2015 - 2017  Dries007 & Double Door Development
  ~
  ~ This program is free software: you can redistribute it and/or modify
  ~ it under the terms of the GNU Affero General Public License as published
  ~ by the Free Software Foundation, either version 3 of the License, or
  ~ (at your option) any later version.
  ~
  ~ This program is distributed in the hope that it will be useful,
  ~ but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~ GNU Affero General Public License for more details.
  ~
  ~ You should have received a copy of the GNU Affero General Public License
  ~ along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
    <link href="/static/css/jquery.terminal.min.css" rel="stylesheet">
    <link rel="shortcut icon" type="image/ico" href="/static/favicon.ico"/>
    <!-- Le JavaScript -->
    <script src="/static/js/jquery.min.js"></script>
    <script src="/static/js/jquery.terminal.min.js"></script>
    <script>
        function wsurl(s)
        {
            var l = window.location;
            return (l.protocol === "https:" ? "wss://" : "ws://") + l.hostname + ":" + l.port + "/socket/" + s;
        }
        $(function () {
            var websocket = new WebSocket(wsurl("serverconsole/${server.ID?js_string}"));
            var term = $('body').terminal(function (command, term) {
                websocket.send(command);
            }, {
                clear: false,
                exit: false,
                history: true,
                convertLinks: true,
                echoCommand: false,
                outputLimit: 10000,
                scrollOnEcho: true,
                wrap: false,
                name: 'console_${server.ID?js_string}',
                greetings: 'Server console for ${server.ID?js_string}.'
            });
            websocket.onerror = function (evt)
            {
                term.error("The websocket errored. Refresh the page!").pause();
                console.error(evt);
            };
            websocket.onclose = function (evt)
            {
                term.error("The websocket closed. Refresh the page!").pause();
                console.error(evt);
            };
            websocket.onmessage = function (evt)
            {
                var temp = JSON.parse(evt.data);
                if (temp.status !== "ok")
                {
                    term.error(temp.message).pause();
                    console.error(temp.message);
                }
                else
                {
                    term.echo(temp.data).resume();
                }
            };
        });
    </script>
</head>
<body style="height: 500px;">
<div id="term"></div>
</body>
</html>