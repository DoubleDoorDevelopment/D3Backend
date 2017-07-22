<div class="panel-body">
    <p>This file has no live updating.</p>

    <form class="form-inline" role="form" onsubmit="makeNew(); return false;">
        <div class="form-group">
            <label class="sr-only" for="newUsername">Username</label>
            <input type="text" class="form-control" id="newUsername" placeholder="Username">
        </div>
        <div class="form-group">
            <label class="sr-only" for="newlvl">Perm lvl</label>
            <input type="number" min="0" class="form-control" id="newlvl" placeholder="Perm lvl" value="4">
        </div>
        <button type="button" class="btn btn-info" onclick="makeNew()">Add</button>
    </form>
    <table class="table table-hover">
        <thead>
        <tr>
            <th class="col-sm-3">Username</th>
            <th class="col-sm-4">UUID</th>
            <th class="col-sm-2">Perm lvl</th>
            <th class="col-sm-3"></th>
        </tr>
        </thead>
        <tbody id="opList">
        </tbody>
    </table>
    <script type="text/javascript">
        var json = ${fm.getFileContents()?js_string};
        var opList = get("opList");
        json.forEach(function (object)
        {
            opList.innerHTML += "<tr id=\"" + object['name'] + "\"><td>" + object['name'] + "</td><td>" + object['uuid'] + "</td><td id=\"" + object['name'] + "lvl\">" + object['level'] + "</td><#if !readonly><td><div class=\"btn-group\"><button type=\"button\" onclick=\"removeUser(\'" + object['name'] + "\')\" class=\"btn btn-danger btn-xs\">Remove</button><button type=\"button\" onclick=\"changePermlvl(\'" + object['name'] + "\')\" class=\"btn btn-warning btn-xs\">Change perm lvl</button></div></td></#if></tr>";
        });

        function isNumber(n)
        {
            return !isNaN(parseInt(n));
        }

        function changePermlvl(username)
        {
            for (var i = json.length - 1; i >= 0; i--)
            {
                if (json[i]["name"] === username)
                {
                    var input = prompt("New perm lvl?", json[i]["level"]);
                    if (input != null && isNumber(input))
                    {
                        get(username + "lvl").innerHTML = input;
                        json[i]["level"] = input;
                    }
                }
            }
        }

        function removeUser(username)
        {
            var element = get(username);
            if (element != null) opList.removeChild(element);
            for (var i = json.length - 1; i >= 0; i--)
            {
                if (json[i]["name"] === username)
                {
                    json.splice(i, 1);
                }
            }
        }

        function makeNew()
        {
            var xmlhttp = new XMLHttpRequest();
            var username = get("newUsername").value;
            var permlvl = get("newlvl").value;

            if (!isNumber(permlvl))
            {
                addAlert("Input invalid.", true);
                return;
            }

            xmlhttp.onreadystatechange = function ()
            {
                if (xmlhttp.readyState == 4)
                {
                    if (xmlhttp.status == 200)
                    {
                        var myArr = JSON.parse(xmlhttp.responseText);
                        if (myArr.hasOwnProperty("id"))
                        {
                            removeUser(username);

                            var uuid = myArr["id"];

                            uuid = [uuid.slice(0, 8), "-", uuid.slice(8, 12), "-", uuid.slice(12, 16), "-", uuid.slice(16, 20), "-", uuid.slice(20)].join('');

                            json.push({"name": username, "uuid": uuid, "level": permlvl});
                            opList.innerHTML += "<tr id=\"" + username + "\"><td>" + username + "</td><td>" + uuid + "</td><td id=\"" + username + "lvl\">" + permlvl + "</td><#if !readonly><td><div class=\"btn-group\"><button type=\"button\" onclick=\"removeUser(\'" + username + "\')\" class=\"btn btn-danger btn-xs\">Remove</button><button type=\"button\" onclick=\"changePermlvl(\'" + username + "\')\" class=\"btn btn-warning btn-xs\">Change perm lvl</button></div></td></#if></tr>";

                            get("newUsername").value = "";
                            get("newlvl").value = "4";
                            return;
                        }
                    }
                    addAlert("Input invalid.", true);
                }
            };
            xmlhttp.open("GET", "https://api.mojang.com/users/profiles/minecraft/" + username, true);
            xmlhttp.send();
        }

        var websocket = new WebSocket(wsurl("filemanager/${server.ID?js_string}/${fm.stripServer(fm.getFile())}"));
        websocket.onerror = function (evt)
        {
            addAlert("The websocket errored. Refresh the page!")
        };
        websocket.onclose = function (evt)
        {
            addAlert("The websocket closed. Refresh the page!")
        };
        websocket.onmessage = function (evt)
        {
            var temp = JSON.parse(evt.data);
            if (temp.status === "ok")
            {
                editor.setValue(temp.data);
                editor.clearSelection();
            }
            else
            {
                addAlert(temp.message);
            }
        };
        function send()
        {
            websocket.send(JSON.stringify({method: "set", args: [JSON.stringify(json)]}));
        }
    </script>
<#if !readonly>
    <button type="button" class="btn btn-primary btn-block" onclick="send()">Save</button>
<#else>
    <p>File is readonly.</p>
</#if>
</div>