<div class="panel-body">
    Click on any value to change it. Changes only apply once the server has been restarted!<br>
    Red values are read only.
</div>
<table class="table table-hover table-condensed">
    <thead>
    <tr>
        <th style="text-align: right;width: 33%">Property</th>
        <th style="text-align: left;">Value</th>
    </tr>
    <tbody id="tableBody">

    </tbody>
</table>
<script>
    const readOnlyProperties = ${Helper.getReadOnlyProperties()};

    function change(field)
    {
        if ($.inArray(field.id, readOnlyProperties) != -1)
        {
            alert("Read only value.");
        }
        else
        {
            var value = prompt('Change value to ?', field.innerHTML);
            if (value != null) chageProperty(field.id, value);
        }
    }

    function chageProperty(property, value)
    {
        websocket.send(property + "=" + value);
    }

    function updateInfo(data)
    {
        Object.keys(data).forEach(function (key)
        {
            var element = document.getElementById(key);
            if (element != null)
            {
                element.innerHTML = data[key];
            }
            else
            {
                document.getElementById("tableBody").innerHTML += "<tr><td style=\"text-align: right;\" id=\"key_" + key + "\">" + key + " = </td><td style=\"cursor: pointer; text-align: left;\" id=\"" + key + "\" onclick=\"change(this)\">" + data[key] + "</td></tr>";
                element = document.getElementById(key);
            }

            if ($.inArray(element.id, readOnlyProperties) != -1)
            {
                element.className = document.getElementById("key_" + key).className = "text-danger";
            }
        });
    }

    var websocket = new WebSocket(wsurl("serverproperties/${server.ID?js_string}"));
    websocket.onmessage = function (evt)
    {
        var temp = JSON.parse(evt.data);
        if (temp.status === "ok")
        {
            updateInfo(temp.data);
        }
        else
        {
            alert(temp.message);
        }
    };
    websocket.onerror = function (evt)
    {
        alert("The websocket errored. Refresh the page!")
    };
    websocket.onclose = function (evt)
    {
        alert("The websocket closed. Refresh the page!")
    };

    $('[rel=tooltip]').tooltip()
</script>