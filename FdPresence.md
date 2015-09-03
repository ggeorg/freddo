## Listing devices in the network ##

```
<!doctype html>
<html>

<head>
    <meta charset="UTF-8">
    <title>Freddo</title>
    <script src="dtalk.js"></script>
</head>

<body onload="DTalk.connect();">

    <pre id="log"></pre>

    <script>
        var app = {

            PRESENCE_SRV: "dtalk.service.Presence",
            VERSION: "1.0",

            startPresenceService: function() {
                var evt = {
                    dtalk: this.VERSION,
                    service: this.SERVICES,
                    action: "start",
                    params: this.PRESENCE_SRV
                };

                DTalk.sendNotification(evt);
            },

            getList: function() {

                var callback = function(evt) {
                    document.getElementById("log").innerHTML = JSON.stringify(evt.data, null, 2);
                };

                var request = {
                    dtalk: this.VERSION,
                    service: this.PRESENCE_SRV,
                    action: "get",
                    params: "list"
                };

                DTalk.sendRequest(request, callback, 3000);
            },

            init: function() {
                this.startPresenceService();
                this.getList();
            }
        };

        DTalk.onopen = function() {
            app.init();
        };
    </script>

</body>

</html>
```

## Listening to Presence changes ##

```
<!doctype html>
<html>

<head>
    <meta charset="UTF-8">
    <title>Freddo</title>
    <script src="dtalk.js"></script>
</head>

<body onload="DTalk.connect();">

    <pre id="log"></pre>

    <script>
        var app = {

            SERVICES: "dtalk.Services",
            PRESENCE_SRV: "dtalk.service.Presence",
            VERSION: "1.0",

            startPresenceService: function() {
                var evt = {
                    dtalk: this.VERSION,
                    service: this.SERVICES,
                    action: "start",
                    params: this.PRESENCE_SRV
                };

                DTalk.sendNotification(evt);
            },

            onPresenceRemoved: function(evt) {
                console.log(evt.data);
                document.getElementById("log").innerHTML = JSON.stringify(evt.data, null, 2);
            },

            onPresenceResolved: function(evt) {
                console.log(evt.data);
                document.getElementById("log").innerHTML = JSON.stringify(evt.data, null, 2);
            },

            init: function() {

                this.startPresenceService();

                DTalk.addEventListener("$" + this.PRESENCE_SRV + ".removed", this.onPresenceRemoved, true);
                DTalk.addEventListener("$" + this.PRESENCE_SRV + ".resolved", this.onPresenceResolved, true);
            }
        };

        DTalk.onopen = function() {
            app.init();
        };
    </script>

</body>

</html>
```