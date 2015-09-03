## Listen to a topic ##

```
<script>
    var app = {

        SRV: "dtalk.MyService",

        callback: function(evt) {
            // do anything with the event
        },

        init: function() {
            DTalk.addEventListener("$" + this.SRV + ".onstatus", this.callback, true);
        },

        clear: function() {
            DTalk.removeEventListener("$" + this.SRV + ".onstatus", this.callback, true);
        }
    };

    DTalk.onopen = function() {
        app.init();
    };

    DTalk.onclose = function() {
        app.clear();
    };
</script>
```