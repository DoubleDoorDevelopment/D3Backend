<!DOCTYPE html>
<!--
  ~ Copyright (c) 2014, DoubleDoorDevelopment
  ~ All rights reserved.
  ~
  ~ Redistribution and use in source and binary forms, with or without
  ~ modification, are permitted provided that the following conditions are met:
  ~
  ~ * Redistributions of source code must retain the above copyright notice, this
  ~   list of conditions and the following disclaimer.
  ~
  ~ * Redistributions in binary form must reproduce the above copyright notice,
  ~   this list of conditions and the following disclaimer in the documentation
  ~   and/or other materials provided with the distribution.
  ~
  ~ * Neither the name of the project nor the names of its
  ~   contributors may be used to endorse or promote products derived from
  ~   this software without specific prior written permission.
  ~
  ~ THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
  ~ AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
  ~ IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  ~ DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
  ~ FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
  ~ DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
  ~ SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
  ~ CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
  ~ OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
  ~ OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  -->
<html>
<head lang="en">
    <meta charset="UTF-8">
    <title>Console ${server.name}</title>
    <!-- Le meta -->
    <meta name="author" content="Dries007">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <!-- Le styles -->
    <link href="/static/css/bootstrap.min.css" rel="stylesheet">
    <link href="/static/css/font-awesome.css" rel="stylesheet">
</head>
<body>
    <b>Note: This is not a real console. This is just a command interface that displays responses.</b>
    <textarea class="textarea form-control" id="text" style="height: 445px;"></textarea>
    <input type="text" class="form-control" placeholder="Command..." onkeydown="if (event.keyCode == 13) sendCommand(this)">
    <script>
        function sendCommand($input)
        {
            xmlhttp=new XMLHttpRequest();
            xmlhttp.open('PUT', window.location.origin + "/console/${server.name}/" + encodeURIComponent($input.value), true)
            xmlhttp.send(null);

            xmlhttp.onreadystatechange=function()
            {
                if (xmlhttp.readyState == 4)
                {
                    if (xmlhttp.status != 200) alert("Error...\n" + xmlhttp.responseText);
                    else document.getElementById("text").value += xmlhttp.responseText + "\n";
                }
            }

            $input.value="";
        }
    </script>
</body>
</html>