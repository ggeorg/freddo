## Action Request ##
```
<script>
    var app = {

        sendActionRequest: function() {

            var callback = function(evt) {
                // do anything with the response
            };

            var actionrequest = {
                dtalk: "1.0",
                service: "dtalk.MyService",
                action: "launch",
                params: {
                    param1: 'my param'
                }
            };

            DTalk.sendRequest(actionrequest, callback, 3000);
        }
    };

    DTalk.onopen = function() {
        app.sendActionRequest();
    };
</script>
```