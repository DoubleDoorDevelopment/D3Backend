</div>
<script src="/static/js/jquery.min.js"></script>
<script src="/static/js/bootstrap.min.js"></script>
<script>
        function navTab($args)
        {
            switch ($args[3])
            {
                case "":
                    document.getElementById("homeNavTab").className += " active"
                    break;
                default:
                    var element = document.getElementById($args[3] + "NavTab");
                    if (element != null) element.className += " active"
                    break;
                case "servers":
                    if ($args.length > 4)
                    {
                        document.getElementById("serversNavTab").className += " active"
                        document.getElementById($args[4] + "NavTab").className += " active"
                    }
                    else
                    {
                        document.getElementById("serverListNavTab").className += " active"
                    }
                    break;

            }
        }
        navTab(document.URL.split("/"));

        var xmlhttp;

        function call()
        {
            execute('PUT', window.location.origin + "/" + Array.prototype.slice.apply(arguments).join("/"));
        }

        function execute($method, $url)
        {
            xmlhttp=new XMLHttpRequest();
            xmlhttp.open($method, $url, true)
            xmlhttp.send(null);

            xmlhttp.onreadystatechange=function()
            {
                if (xmlhttp.readyState == 4)
                {
                    if (xmlhttp.status != 200) alert("Error...\n" + xmlhttp.responseText);
                    else setTimeout(function(){location.reload()}, 2500);
                }
            }
        }
    </script>
</body>
</html>