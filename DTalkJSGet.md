## Get Request ##

```
<script>
    var app = {

        sendGetRequest: function() {

            var callback = function(evt) {
                // do anything with the response
            };

            var getrequest = {
                dtalk: "1.0",
                service: "dtalk.MyService",
                action: "get",
                params: "info"
            };

            DTalk.sendRequest(getrequest, callback, 3000);
        }
    };

    DTalk.onopen = function() {
        app.sendGetRequest();
    };
</script>
```