<#setting url_escaping_charset="UTF-8">
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
    <title>D3 Backend</title>
    <!-- Le meta -->
    <meta name="author" content="Dries007">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <!-- Le styles -->
    <link href="/static/css/bootstrap.min.css" rel="stylesheet">
    <link href="/static/css/font-awesome.min.css" rel="stylesheet">
    <link rel="shortcut icon" type="image/ico" href="/static/favicon.ico"/>
    <style>
        body {
            padding-top: 70px;
        }

        .hiddenlink a:link {
            color: #000000;
            text-decoration: none
        }

        .hiddenlink a:visited {
            color: #000000;
            text-decoration: none
        }

        .hiddenlink a:hover {
            color: #3366CC;
            text-decoration: underline
        }

        .hiddenlink a:active {
            color: #000000;
            text-decoration: none
        }
    </style>
    <script src="/static/js/jquery.min.js"></script>
    <script src="/static/js/bootstrap.min.js"></script>
    <script>
        function get(id)
        {
            return document.getElementById(id);
        }

        function addAlert(message, dismissible)
        {
            var alert = "<div class=\"alert alert-danger\" role=\"alert\">";
            if (dismissible) alert += "<button type=\"button\" class=\"close\" data-dismiss=\"alert\" aria-label=\"Close\">&times;</button>";
            get("alerts").innerHTML += alert + message + "</div>"
        }

        function wsurl(s)
        {
            var l = window.location;
            return (l.protocol === "https:" ? "wss://" : "ws://") + l.hostname + ":" + l.port + "/socket/" + s;
        }

        function openPopup(url)
        {
            window.open(window.location.origin + url, '_new', 'height=500,width=800');
        }

        function call(url, method, args, func)
        {
            var cmdwebsocket = new WebSocket(wsurl(url));
            cmdwebsocket.onopen = function (evt)
            {
                cmdwebsocket.send(JSON.stringify({method: method, args: args}));
            };
            cmdwebsocket.onmessage = function (evt)
            {
                var temp = JSON.parse(evt.data);
                if (typeof func === 'undefined')
                {
                    if (temp.status !== "ok") addAlert(temp.message, true);
                }
                else
                {
                    func(temp.data);
                }
            };
            cmdwebsocket.onerror = function (evt)
            {
                addAlert("The call socket connection errored. Try again.", true);
            };
        }

        function reload()
        {
            setTimeout(function () { location.reload(true); }, 1);
        }

        function navTab($args)
        {
            switch ($args[3])
            {
                case "":
                    get("homeNavTab").className += " active";
                    break;
                default:
                    var element = get($args[3] + "NavTab");
                    if (element != null) element.className += " active";
                    break;
                case "servers":
                    if ($args.length > 4)
                    {
                        get("serversNavTab").className += " active";
                        get($args[4] + "NavTab").className += " active";
                    }
                    else
                    {
                        get("serverListNavTab").className += " active";
                    }
                    break;
            }
        }
        navTab(document.URL.split("/"));
        $(function ()
        {
            $('.panel-heading span.clickable').on("click", function (e)
            {
                if ($(this).hasClass('panel-collapsed'))
                {
                    // expand the panel
                    $(this).parents('.panel').find('.panel-body').slideDown();
                    $(this).removeClass('panel-collapsed');
                    $(this).find('i').removeClass('fa-chevron-down').addClass('fa-chevron-up');
                }
                else
                {
                    // collapse the panel
                    $(this).parents('.panel').find('.panel-body').slideUp();
                    $(this).addClass('panel-collapsed');
                    $(this).find('i').removeClass('fa-chevron-up').addClass('fa-chevron-down');
                }
            });
        });
    </script>
</head>
<body>
<!-- Fixed navbar -->
<div class="navbar navbar-default navbar-fixed-top" role="navigation">
    <div class="container">
        <div class="navbar-header">
            <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target=".navbar-collapse">
                <span class="sr-only">Toggle navigation</span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>
            <a class="navbar-brand" href="/">D3 Backend</a>
        </div>
        <div class="navbar-collapse collapse">
            <ul class="nav navbar-nav">
                <li id="homeNavTab"><a href="/">Home</a></li>
            <#if user??>
                <li id="serverListNavTab"><a href="/servers">Server List</a></li>
                <li id="serversNavTab" class="dropdown">
                    <a href="/servers" class="dropdown-toggle" data-toggle="dropdown">Servers <span
                            class="caret"></span></a>
                    <ul class="dropdown-menu" role="menu">
                        <#list Settings.servers as server>
                            <#if server.canUserControl(user)>
                                <li id="${server.ID?url}NavTab"><a href="/server?server=${server.ID?url}">${server.ID?html}</a></li>
                            </#if>
                        </#list>
                    </ul>
                </li>
                <li id="newserverListNavTab"><a href="/newserver">New Server</a></li>
                <li id="usersNavTab"><a href="/users">Users</a></li>
                <#if user.isAdmin()>
                    <li id="consoleNavTab"><a href="/console">Console</a></li>
                </#if>
            </#if>
            </ul>
            <ul class="nav navbar-nav navbar-right">
                <li id="loginNavTab"><a href="/login"><#if user??>${user.username}<#else>Log in</#if></a></li>
            </ul>
        </div>
        <!--/.nav-collapse -->
    </div>
</div>
<div class="container" id="container">
    <div id="alerts"></div>