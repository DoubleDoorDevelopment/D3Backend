<form class="form-inline" role="form" onsubmit="makeNew(); return false;">
    <div class="form-group">
        <label class="sr-only" for="newUsername">Username</label>
        <input type="text" class="form-control" id="newUsername" placeholder="Username">
    </div>
    <button type="button" class="btn btn-info" onclick="makeNew()">Add</button>
</form>
<table class="table table-hover">
    <thead>
    <tr>
        <th class="col-sm-3">Username</th>
        <th class="col-sm-4">UUID</th>
        <th class="col-sm-5"></th>
    </tr>
    </thead>
    <tbody id="opList">
    </tbody>
</table>
<script type="text/javascript">
    var json = ${fm.getFileContentsAsJson()};
    var opList = document.getElementById("opList");
    json.forEach(function (object) {
        opList.innerHTML += "<tr id=\"" + object['name'] + "\"><td>" + object['name'] + "</td><td>" + object['uuid'] + "</td><#if !readonly><td><div class=\"btn-group\"><button type=\"button\" onclick=\"removeUser(\'" + object['name'] + "\')\" class=\"btn btn-danger btn-xs\">Remove</button></div></td></#if></tr>";
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

        xmlhttp.onreadystatechange = function () {
            if (xmlhttp.readyState == 4) {
                if (xmlhttp.status == 200) {
                    var myArr = JSON.parse(xmlhttp.responseText);
                    if (myArr.hasOwnProperty("id")) {
                        removeUser(username)

                        var uuid = myArr["id"];

                        uuid = [uuid.slice(0, 8), "-", uuid.slice(8, 12), "-", uuid.slice(12, 16), "-", uuid.slice(16, 20), "-", uuid.slice(20)].join('')

                        json.push({"name": username, "uuid": uuid});
                        opList.innerHTML += "<tr id=\"" + username + "\"><td>" + username + "</td><td>" + uuid + "</td><#if !readonly><td><div class=\"btn-group\"><button type=\"button\" onclick=\"removeUser(\'" + username + "\')\" class=\"btn btn-danger btn-xs\">Remove</button></div></td></#if></tr>";

                        document.getElementById("newUsername").value = "";
                        return;
                    }
                }
                alert("Input invalid.");
            }
        }
        xmlhttp.open("GET", "https://api.mojang.com/users/profiles/minecraft/" + username, true);
        xmlhttp.send();
    }
</script>
<#if !readonly>
<button type="button" class="btn btn-primary btn-block" onclick="call('filemanager', '${fm.server.ID}', '${fm.stripServer(fm.file)}', 'set', JSON.stringify(json))">Save</button>
<#else>
<p>File is readonly.</p>
</#if>