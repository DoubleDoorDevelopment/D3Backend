<#include "header.ftl">
<textarea class="textarea form-control" id="text" style="height: 800px;" wrap="off"></textarea>
<p>Ps: You can't give any input from this page. You need SSH access for that.</p>
<script>
    var callURL = window.location.origin + "/backendConsoleText";
    var lines = 0;
    var textarea = document.getElementById('text');
    var autoScroll = true;

    var getConsoleText = function()
    {
        xmlhttp = new XMLHttpRequest();
        xmlhttp.open("GET", callURL  + "/" + lines , true)
        xmlhttp.setRequestHeader("content-type", "application/x-www-form-urlencoded")
        xmlhttp.send(null);

        xmlhttp.onreadystatechange = function () {
            if (xmlhttp.readyState == 4) {
                if (xmlhttp.status != 200) alert("Error...\n" + xmlhttp.responseText);
                else
                {
                    autoScroll = textarea.scrollHeight <= textarea.scrollTop + 800;
                    responce = JSON.parse(xmlhttp.responseText);
                    textarea.value += responce["text"];
                    lines = responce["size"];
                    if (autoScroll) textarea.scrollTop = textarea.scrollHeight;
                }
            }
        }
    };
    setInterval(getConsoleText, 5000);
    getConsoleText();
</script>
<#include "footer.ftl">