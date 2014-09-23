</div>
<script src="/static/js/jquery.min.js"></script>
<script src="/static/js/bootstrap.min.js"></script>
<script src="/static/js/commands.js"></script>
<script>
    function navTab($args) {
        switch ($args[3]) {
            case "":
                document.getElementById("homeNavTab").className += " active"
                break;
            default:
                var element = document.getElementById($args[3] + "NavTab");
                if (element != null) element.className += " active"
                break;
            case "servers":
                if ($args.length > 4) {
                    document.getElementById("serversNavTab").className += " active"
                    document.getElementById($args[4] + "NavTab").className += " active"
                }
                else {
                    document.getElementById("serverListNavTab").className += " active"
                }
                break;

        }
    }
    navTab(document.URL.split("/"));

    var xmlhttp;

    function call() {
        var args = Array.prototype.slice.apply(arguments);
        execute('PUT', window.location.origin, args, setTimeout(function () {
            location.reload()
        }, 2500));
    }

    jQuery(function ($) {
        $('.panel-heading span.clickable').on("click", function (e) {
            if ($(this).hasClass('panel-collapsed')) {
                // expand the panel
                $(this).parents('.panel').find('.panel-body').slideDown();
                $(this).removeClass('panel-collapsed');
                $(this).find('i').removeClass('fa-chevron-down').addClass('fa-chevron-up');
            }
            else {
                // collapse the panel
                $(this).parents('.panel').find('.panel-body').slideUp();
                $(this).addClass('panel-collapsed');
                $(this).find('i').removeClass('fa-chevron-up').addClass('fa-chevron-down');
            }
        });
    });
</script>
</body>
</html>