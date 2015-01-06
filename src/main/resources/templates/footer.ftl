<footer>
    <p class="text-center">
    <#if Helper.hasUpdate()><span class="text-danger"><b>Backend is out of date!</b> Latest version: ${Helper.getUpdateVersion()}</span><br></#if>
        <span class="text-muted">Build with D3 Backend.</span>
    </p>
</footer>
</div>
<script>
    function navTab($args)
    {
        switch ($args[3])
        {
            case "":
                document.getElementById("homeNavTab").className += " active";
                break;
            default:
                var element = document.getElementById($args[3] + "NavTab");
                if (element != null) element.className += " active";
                break;
            case "servers":
                if ($args.length > 4)
                {
                    document.getElementById("serversNavTab").className += " active";
                    document.getElementById($args[4] + "NavTab").className += " active";
                }
                else
                {
                    document.getElementById("serverListNavTab").className += " active";
                }
                break;

        }
    }
    navTab(document.URL.split("/"));

    jQuery(function ($)
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

    <#if server??>
    if (websocketMonitor.onmessage == null)
    {
        websocketMonitor.onmessage = function (evt)
        {
            var temp = JSON.parse(evt.data);
            if (temp.status === "ok")
            {
                var onlineDom = document.getElementById("online");
                if (onlineDom != null)
                {
                    onlineDom.innerHTML = temp.data.online ? "Online" : "Offline";
                    onlineDom.className = "label label-" + (temp.data.online ? "success" : "danger");
                }
            }
            else
            {
                alert(temp.message);
            }
        }
    }
    </#if>
</script>
</body>
</html>