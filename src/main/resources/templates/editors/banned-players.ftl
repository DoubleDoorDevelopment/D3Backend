<div class="panel-body">
    <p>This file has no live updating.</p>
    <form class="form-inline" role="form" onsubmit="makeNew(); return false;">
        <div class="form-group">
            <label class="sr-only" for="newUsername">Username</label>
            <input type="text" class="form-control" id="newUsername" placeholder="Username">
        </div>
        <div class="form-group">
            <label class="sr-only" for="newSource">Source</label>
            <input type="text" class="form-control" id="newSource" placeholder="Source" value="Server">
        </div>
        <div class="form-group">
            <label class="sr-only" for="newReason">Reason</label>
            <input type="text" class="form-control" id="newReason" placeholder="Reason" value="Banned by an operator.">
        </div>
        <div class="form-group">
            <label class="sr-only" for="newCreated">Reason</label>
            <input type="text" class="form-control" id="newCreated" placeholder="Data Created" value="${Helper.getNowInBanFormat()}">
        </div>
        <div class="form-group">
            <label class="sr-only" for="newExpires">Reason</label>
            <input type="text" class="form-control" id="newExpires" placeholder="Expires" value="forever">
        </div>
        <button type="button" class="btn btn-info" id="addBtn" onclick="makeNew()">Add</button>
    </form>
    <table class="table table-hover">
        <thead>
        <tr>
            <th class="col-sm-1">Username</th>
            <th class="col-sm-3">UUID</th>
            <th class="col-sm-1">Source</th>
            <th class="col-sm-2">Reason</th>
            <th class="col-sm-2">Created</th>
            <th class="col-sm-1">Expires</th>
            <th class="col-sm-1"></th>
        </tr>
        </thead>
        <tbody id="opList">
        </tbody>
    </table>
    <script type="text/javascript">
        var json = ${fm.getFileContents()};
        var opList = document.getElementById("opList");
        json.forEach(function (object) {
            opList.innerHTML +=
                    "<tr id=\"" + object['name'] + "\">" +
                    "<td>" + object['name'] + "</td>" +
                    "<td>" + object['uuid'] + "</td>" +
                    "<td>" + object['source'] + "</td>" +
                    "<td>" + object['reason'] + "</td>" +
                    "<td>" + object['created'] + "</td>" +
                    "<td>" + object['expires'] + "</td>" +
            <#if !readonly>
                    "<td>" +
                    "<div class=\"btn-group\">" +
                    "<button type=\"button\" onclick=\"removeUser(\'" + object['name'] + "\')\" class=\"btn btn-danger btn-xs\">Del</button>" +
                    "<button type=\"button\" onclick=\"makeEditable(\'" + object['name'] + "\')\" class=\"btn btn-warning btn-xs\">Edit</button>" +
                    "</div>" +
                    "</td>"+
            </#if>
                    "</tr>";
        });

        function removeUser(username) {
            var element = document.getElementById(username);
            if (element != null) opList.removeChild(element);
            for (var i = json.length - 1; i >= 0; i--) {
                if (json[i]["name"] === username) {
                    json.splice(i, 1);
                }
            }
        }

        function makeNew() {
            var xmlhttp = new XMLHttpRequest();
            var username = document.getElementById("newUsername").value;
            var source = document.getElementById("newSource").value;
            var reason = document.getElementById("newReason").value;
            var created = document.getElementById("newCreated").value;
            var expires = document.getElementById("newExpires").value;

            xmlhttp.onreadystatechange = function () {
                if (xmlhttp.readyState == 4) {
                    if (xmlhttp.status == 200) {
                        var myArr = JSON.parse(xmlhttp.responseText);
                        if (myArr.hasOwnProperty("id")) {
                            removeUser(username)

                            var uuid = myArr["id"];

                            uuid = [uuid.slice(0, 8), "-", uuid.slice(8, 12), "-", uuid.slice(12, 16), "-", uuid.slice(16, 20), "-", uuid.slice(20)].join('')

                            json.push({"name": username, "uuid": uuid, "source": source, "reason": reason, "created": created, "expires": expires});
                            opList.innerHTML +=
                                    "<tr id=\"" + username + "\">" +
                                    "<td>" + username + "</td>" +
                                    "<td>" + uuid + "</td>" +
                                    "<td>" + source + "</td>" +
                                    "<td>" + reason + "</td>" +
                                    "<td>" + created + "</td>" +
                                    "<td>" + expires + "</td>" +
                            <#if !readonly>
                                    "<td>" +
                                    "<div class=\"btn-group\">" +
                                    "<button type=\"button\" onclick=\"removeUser(\'" + username + "\')\" class=\"btn btn-danger btn-xs\">Del</button>" +
                                    "<button type=\"button\" onclick=\"makeEditable(\'" + username + "\')\" class=\"btn btn-warning btn-xs\">Edit</button>" +
                                    "</div>" +
                                    "</td>" +
                            </#if>
                                    "</tr>";

                            document.getElementById("newUsername").value = "";
                            document.getElementById("newSource").value = "Server";
                            document.getElementById("newReason").value = "Banned by an operator.";
                            document.getElementById("newCreated").value = "${Helper.getNowInBanFormat()}";
                            document.getElementById("newExpires").value = "forever";

                            document.getElementById("addBtn").innerHTML = "Add";
                            return;
                        }
                    }
                    alert("Input invalid.");
                }
            }
            xmlhttp.open("GET", "https://api.mojang.com/users/profiles/minecraft/" + username, true);
            xmlhttp.send();
        }

        function makeEditable(username) {
            var element = document.getElementById(username);
            if (element != null) opList.removeChild(element);
            for (var i = json.length - 1; i >= 0; i--) {
                if (json[i]["name"] === username) {
                    document.getElementById("newUsername").value = json[i]["name"];
                    document.getElementById("newSource").value = json[i]["source"];
                    document.getElementById("newReason").value = json[i]["reason"];
                    document.getElementById("newCreated").value = json[i]["created"];
                    document.getElementById("newExpires").value = json[i]["expires"];

                    document.getElementById("addBtn").innerHTML = "Re-add";

                    json.splice(i, 1);
                }
            }
        }
    </script>
    <#if !readonly>
    <button type="button" class="btn btn-primary btn-block" onclick="callNoRefresh('filemanager', '${fm.server.ID}', '${fm.stripServer(fm.file)}', 'set', JSON.stringify(json))">Save</button>
    <#else>
    <p>File is readonly.</p>
    </#if>
</div>