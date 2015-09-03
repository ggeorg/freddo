# Messaging Protocol #



# Introduction #

DTalk messaging protocol arose from a need to allow a mobile app developer to access native device services such as the camera or accelerometer from JavaScript as well as device-to-device communication.

Over time DTalk messaging protocol has matured into a protocol which can be used past these simple use cases, but still maintains its core design principles of simplicity and interoperability.

DTalk is based on WebSocket protocol which provides the reliable 2-way streaming protocol. But, DTalk messaging protocol is transport agnostic in that the concepts can be used within the same process, over sockets, or in many various bidirectional streaming protocols.

# Details #

The general mechanism consists of two peers establishing a data connection. During the lifetime of a connection, peers may invoke actions provided by the other peer or publish events. To invoke a remote action, a request is sent. Unless the request is a notification it must be replied to with a response. Events are published to topics, which interested parts can subscribe to.

## Request Object ##

A remote action is requested by sending a request object to a remote service. The request object has the following members:

| **dtalk** | A string specifying the version of the DTalk protocol.<br>MUST be "1.0". <br>
<tr><td> <b>service</b> </td><td> A string containing the name of the remote service to send the request to. </td></tr>
<tr><td> <b>action</b> </td><td> A string containing the name of the requested action to be invoked.      </td></tr>
<tr><td> <b>params</b> </td><td> A primitive os structured value that contains additional information to be used during the processing of the requested action.<br>This member MAY be omitted. </td></tr>
<tr><td> <b>id</b> </td><td> An identifier established by the client that MUST contain a string if included. <br>If 'id' is not included it is assumed to be a notification. </td></tr></tbody></table>

In case of device-to-device communication two additional members are used:<br>
<br>
<table><thead><th> <b>to</b> </th><th> The device name, where the remote service is running. </th></thead><tbody>
<tr><td> <b>from</b> </td><td> The device name where the sender is running.          </td></tr></tbody></table>

<h2>Notification Object</h2>

A notification is a special request object without an "id" member. A request object that is a notification signifies the client's lack of interest in the corresponding response object, and as such no response object needs to be returned to the client.<br>
<br>
Notifications are not confirmable by definition, since they do not have a response object to be returned. As such, the client would not be aware of any errors.<br>
<br>
<h2>Event Object</h2>

An event is a special notification object send to subscribed parties. The event object does not have an "action" member and in that sense the event object purpose is informational only.<br>
<br>
To subscribe to a topic interested parties have to send a "subscribe" request to "dtalk.Dispatcher" service:<br>
<br>
<pre><code>var subscribeReq = {<br>
   service : "dtalk.Dispatcher",<br>
   action : "subscribe",<br>
   params : topic<br>
};<br>
DTalk.sendNotification(subscribeReq);<br>
</code></pre>

<h2>Response Object</h2>

When the action request completes, the service must reply with a response object. The response is expresses as a single JSON object, with the following members:<br>
<br>
<table><thead><th> <b>dtalk</b> </th><th> A string specifying the version of the DTalk protocol (currently "1.0"). </th></thead><tbody>
<tr><td> <b>service</b> </td><td> This member is REQUIRED.<br>It MUST be the same as the value of the 'id' member in the request object.<br>If there was an error in detecting the id in the request object (e.g. parse error) the response is not send - in that case the client MUST receive a timeout. </td></tr>
<tr><td> <b>result</b> </td><td> This member is REQUIRED on success.<br>This member MUST NOT exist if there was an error invoking the action.<br>The value of this member is determined by the requested action. </td></tr>
<tr><td> <b>error</b> </td><td> This member is REQUIRED on error.<br>This member MUST NOT exist if there was no error triggered during invocation.<br>The value for this member MUST be an object as defined below. </td></tr></tbody></table>

Either the result member or error member MUST be included, but both members MUST NOT be included.<br>
<br>
<h3>Error Object</h3>

When a DTalk request encounters an error, the response object MUST contain the error member with a value that is an object with the following members:<br>
<br>
<table><thead><th> <b>code</b> </th><th> A number that indicates the error type that occured.<br>This MUST be an integer. </th></thead><tbody>
<tr><td> <b>message</b> </td><td> A string providing a short description of the error.                             </td></tr>
<tr><td> <b>data</b> </td><td> A primitive os structured value that contains additional information about the error.<br>This may be omitted.<br>The value of this member is defined by the service (e.g. detailed error information, nested errors etc.). </td></tr></tbody></table>

The error codes from and including -32768 to -32000 are reserved for pre-defined errors. Any code within this range, but not defined explicitly below is reserved for future use.<br>
<br>
<table><thead><th> <b>code</b> </th><th> <b>message</b> </th><th> <b>meaning</b> </th></thead><tbody>
<tr><td> -32700      </td><td> Parse error    </td><td> Invalid JSON was received by the service.<br>An error occurred on the server while parsing the JSON text. </td></tr>
<tr><td> -32600      </td><td> Invalid Request </td><td> The JSON sent is not a valid request object. </td></tr>
<tr><td> -32601      </td><td> Action not found </td><td> The action does not exists / is not available. </td></tr>
<tr><td> -32602      </td><td> Invalid params </td><td> Invalid action parameter(s). </td></tr>
<tr><td> -32603      </td><td> Internal error </td><td> Internal DTalk error. </td></tr>
<tr><td> -32000 to -32099 </td><td> Server error   </td><td> Reserved for implementation-defined server-errors. </td></tr></tbody></table>

<h2>Message-Broker</h2>

Out of the box, a "simple" message broker is used to send messages to subscribers. In fact DTalk messaging protocol is build on top of a pub/subscribe message bus.<br>
<br>
The "service" member in the request object defines the topic where the message is published to.<br>
<br>
Sending a response to a request is easy, get the "id" member of the request and publish the response to that topic. The value of the "service" member in the response object MUST be the value of the "id" member in the request object. This pattern is using a temporary queue, once the client receives the response the temporary queue is destroyed.<br>
<br>
To use the internal message broker and define other high level protocols this is possible by sending "subscribe"/"unsubscribe" requests to the "dtalk.Dispatcher" service.<br>
<br>
<h1>See Also</h1>

<ol><li>DTalk messaging protocol derived from <a href='http://www.jsonrpc.org/specification'>JSON-RPC</a>.<br>
</li><li><a href='http://stefaniuk.github.io/json-service/'>http://stefaniuk.github.io/json-service/</a>
</li><li><a href='http://en.wikipedia.org/wiki/JSON-WSP'>http://en.wikipedia.org/wiki/JSON-WSP</a>