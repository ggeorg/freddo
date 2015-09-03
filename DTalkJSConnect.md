## How to connect to a WebSocket using DTalk ##

The the following code shows how to open a connection with the WebSocket. This HTML page expects the query parameters to contain the  web socket address with the following format:

`ws=ws://[ip_address]:[port_number]/dtalksrv`

Example: http://localhost/index.html?ws=ws://10.0.0.2:3456/dtalksrv

```
<!doctype html>
<html>

<head>
    <meta charset="UTF-8">
    <title>Freddo</title>
    <script src="dtalk.js"></script>
</head>

<body onload="DTalk.connect();">

    <div id="log"></div>

    <script>
        DTalk.onopen = function() {
            document.getElementById("log").innerHTML = "<span style='color:green'>WEBSOCKET OPENED</span>";
        };

        DTalk.onclose = function() {
            document.getElementById("log").innerHTML = "<span style='color:red'>WEBSOCKET CLOSED</span>";
        };
    </script>

</body>

</html>

```