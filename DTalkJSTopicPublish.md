## Publish a Topic ##

```
<script>
    var app = {

        TOPIC: "dtalk.MyService.onstatus",

        publish: function() {
            var evt = {
                dtalk: "1.0",
                service: "$" + TOPIC,
                params: {
                    data: 'My data'
                }
            };
            DTalk.sendNotification(evt);
        }
    };

    DTalk.onopen = function() {
        app.publish();
    };
</script>
```