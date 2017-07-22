<footer>
    <p class="text-center">
        <#if Helper.hasUpdate()><span class="text-danger"><b>Backend is out of date!</b> Latest version: ${Helper.getUpdateVersion()}</span><br></#if>
        <span class="text-muted">D3 Backend v${Helper.getVersionString()} &mdash; Server Time: ${Helper.getServerTime()}</span>
    </p>
</footer>
</div>
</body>
</html>