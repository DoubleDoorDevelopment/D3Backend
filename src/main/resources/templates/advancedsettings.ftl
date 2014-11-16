<#include "header.ftl">
<#assign restartInfo = server.getRestartingInfo()>
<h1>Advanced Settings
    <small> <a href="/server?server=${server.ID}">${server.ID}</a>   <span id="online"></span></small>
</h1>
<div class="row">
    <div class="col-sm-6">
        <div class="panel panel-info">
            <div class="panel-heading">
                <h3 class="panel-title" style="text-align: center;">Restart info</h3>
            </div>
            <div class="panel-body" style="text-align: center;">
                <p>Last autorestart: ${restartInfo.getLastRestart("YYYY-MM-dd hh:mm:ss")}</p>
                <div class="form-group">
                    <label for="inputGlobalRestartTimeout">Global Restart Timout</label>
                    <div class="input-group">
                        <input id="inputGlobalRestartTimeout" class="form-control" aria-describedby="helpBlock" type="number" min="0" placeholder="0">
                        <div class="input-group-addon">hours</div>
                    </div>
                </div>
                <span id="inputGlobalRestartTimeoutHelp" class="help-block">Minimun time inbetween automated backups.</span>
                <button id="submit" type="submit" class="btn btn-primary" onclick="sendRestartInfo();">Save!</button>
            </div>
        </div>
    </div>
</div>
<script>
    function sendRestartInfo()
    {
        console.info("sendRestartInfo");
    }
</script>
<#include "footer.ftl">
