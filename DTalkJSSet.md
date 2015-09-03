## Set Request ##

```
<script>
    var app = {

        sendSetRequest: function() {

            var callback = function(evt) {
                // do anything with the response
            };

            var setrequest = {
                dtalk: "1.0",
                service: "dtalk.MyService",
                action: "set",
                params: {
                    "info": "connected"
                }
            };

            DTalk.sendRequest(setrequest, callback, 3000);
        }
    };

    DTalk.onopen = function() {
        app.sendSetRequest();
    };
</script>
```