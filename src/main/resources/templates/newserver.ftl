<#include "header.ftl">
<#if message??>
    <div class="alert alert-danger" role="alert">${message}</div>
</#if>
<form class="form-horizontal" name="form" role="form" method="post" onsubmit="return validateForm()">
    <#if user.isAdmin()>
        <div class="form-group">
            <label for="owner" class="col-sm-2 control-label">Owner</label>

            <div class="col-sm-10">
                <select name="owner" id="owner" class="form-control">
                    <#list Settings.users as user1>
                        <option <#if user1 == user>selected="selected"</#if>>${user1.username}</option>
                    </#list>
                </select>
            </div>
        </div>
    </#if>
    <div class="form-group" id="name-div">
        <label for="name" class="col-sm-2 control-label">Server Name</label>

        <div class="col-sm-10">
            <input type="text" class="form-control" id="name" name="name" required onchange="checkName()">
            <span class="help-block" id="name-help">"${user.username}-" will be used as a prefix automatically.</span>
        </div>
    </div>
    <#if !Settings.fixedPorts>
        <div class="form-group">
            <label for="serverport" class="col-sm-2 control-label">Server Port</label>

            <div class="col-sm-10">
                <#assign serverport = Settings.portRange.getNextAvailablePort()>
                <input type="number" min="1" max="65535" class="form-control" id="serverport" name="serverport"
                       value="${serverport?c}" required>
            </div>
        </div>
        <div class="form-group">
            <label for="rconport" class="col-sm-2 control-label">RCon Port</label>

            <div class="col-sm-10">
                <input type="number" min="1" max="65535" class="form-control" id="rconport" name="rconport"
                       value="${Settings.portRange.getNextAvailablePort(serverport)?c}" required>
            </div>
        </div>
    </#if>
    <#if !Settings.fixedIP>
        <div class="form-group">
            <label for="ip" class="col-sm-2 control-label">Hostname / IP</label>

            <div class="col-sm-10">
                <input type="text" class="form-control" id="ip" name="ip" placeholder="localhost">
            </div>
        </div>
    </#if>
    <#assign maxRam = user.getMaxRamLeft()>
    <div class="form-group">
        <label for="RAMmin" class="col-sm-2 control-label">RAM min (in MB)</label>

        <div class="col-sm-10">
            <input type="number" min="0" <#if maxRam != -1>max="${maxRam?c}" value="${maxRam?c}"</#if>
                   class="form-control" id="RAMmin" name="RAMmin" required>
        </div>
    </div>
    <div class="form-group">
        <label for="RAMmax" class="col-sm-2 control-label">RAM max (in MB)</label>

        <div class="col-sm-10">
            <input type="number" min="0" <#if maxRam != -1>max="${maxRam?c}" value="${maxRam?c}"</#if>
                   class="form-control" id="RAMmax" name="RAMmax" required>
        </div>
    </div>
    <div class="form-group">
        <label for="PermGen" class="col-sm-2 control-label">PermGen (in MB)</label>

        <div class="col-sm-10">
            <input type="number" min="64" max="512" value="64" class="form-control" id="PermGen" name="PermGen" required>
        </div>
    </div>
    <div class="form-group">
        <label for="extraJavaParameters" class="col-sm-2 control-label">Extra Java parameters</label>

        <div class="col-sm-10">
            <textarea class="form-control" rows="3" id="extraJavaParameters" name="extraJavaParameters"></textarea>
            <span class="help-block">One per line!</span>
        </div>
    </div>
    <div class="form-group">
        <label for="extraMCParameters" class="col-sm-2 control-label">Extra MC parameters</label>

        <div class="col-sm-10">
            <textarea class="form-control" rows="3" id="extraMCParameters" name="extraMCParameters"></textarea>
            <span class="help-block">One per line!</span>
        </div>
    </div>
    <div class="form-group">
        <label for="jarname" class="col-sm-2 control-label">Jar name</label>

        <div class="col-sm-10">
            <input type="text" class="form-control" id="jarname" name="jarname" value="minecraft_server.jar">
        </div>
    </div>
    <div class="form-group">
        <label for="rconpass" class="col-sm-2 control-label">RCon password</label>

        <div class="col-sm-10">
            <input type="text" class="form-control" id="rconpass" name="rconpass" value="${Helper.randomString(10)}">
        </div>
    </div>
    <div class="form-group">
        <label for="admins" class="col-sm-2 control-label">Admins</label>

        <div class="col-sm-10">
            <textarea class="form-control" rows="3" id="admins" name="admins"></textarea>
            <span class="help-block">Don't include yourself... One per line!</span>
        </div>
    </div>
    <div class="form-group">
        <label for="coOwners" class="col-sm-2 control-label">Co Owners</label>

        <div class="col-sm-10">
            <textarea class="form-control" rows="3" id="coOwners" name="coOwners"></textarea>
            <span class="help-block">Don't include yourself... One per line!</span>
        </div>
    </div>
    <div class="form-group">
        <label for="autostart" class="col-sm-2 control-label"></label>

        <div class="col-sm-10">
            <input type="checkbox" id="autostart" name="autostart"> Autostart
        </div>
    </div>
    <!-- submit btn -->
    <div class="form-group">
        <div class="col-sm-offset-2 col-sm-10">
            <button id="submit" type="submit" class="btn btn-primary">Submit!</button>
            <span class="help-block">By clicking submit you indicate that you agree to <a href="https://account.mojang.com/documents/minecraft_eula">Mojang's EULA.</a></span>
        </div>
    </div>
</form>
<script>
    function checkName() {
        var name = document.getElementById("name").value;
        while (!name.match(/^[\w]+$/)) {
            name = prompt('Your server ID contains non allowed characters.\nPlease remove all spaces and special characters.', name);
            if (name == null) break;
        }

        if (name == null || !name.match(/^[\w]+$/)) {
            document.getElementById("submit").disabled = true;
            document.getElementById("name-div").className = "form-group has-error";

        }
        else {
            document.getElementById("name").value = name;
            document.getElementById("submit").disabled = false;
            document.getElementById("name-div").className = "form-group";
        }
    }
</script>
<#include "footer.ftl">
