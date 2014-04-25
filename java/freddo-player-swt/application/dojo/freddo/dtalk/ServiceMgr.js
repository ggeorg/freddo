define(["./DTalkAdapter"], function(DTalkAdapter) {

	var DtalkAdapter = {
		_srv: "dtalk.Services",
		_srvListener: "$dtalk.Services",
		
		start: function(service, callback, to, timeout) {
			DTalkAdapter.invokeWithCallback(this._srv, "start", service, callback, to, timeout);
		},
		
		stop: function(service, to) {
			DTalkAdapter.invoke(this._srv, "stop", service, to)
		}
	};
	
	return DtalkAdapter;
	
});