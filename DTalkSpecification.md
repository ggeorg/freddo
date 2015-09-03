# DTalk Specification #



# Introduction #

This specification is similar to [Link-Local XMPP](http://xmpp.org/extensions/xep-0174.html). The specification defines how to communicate over local or wide-area networks using the principles of zero-configuration for endpoint discovery and the syntax of JSON objects and [JSON based messaging](MessagingProtocol.md) for real time communication. DTalk uses DNS-based Service Discovery and Multicast DNS to discover entities that support the protocol, including their IP addresses and preferred ports.

Serverless messaging is typically restricted to a local network (or ad-hoc wide-area network) because of how zero-configuration works.

# Details #

DTalk supports four fundamental operations, each of which is a necessary part of zero-configuration network services and server less messaging:

  * Publication (advertising a service)
  * Discovery (browsing for available services)
  * Resolution (translating service instance names to addresses and port numbers for use)
  * Messaging (exchange of messages between two entities)

These operations are discussed in detail in the following sections.

## Publication ##

To publish a service, an application or device must register the service with a Multicast DNS responder. When a service is registered, three related DNS records are created: a service (SRV) record, a pointer (PTR) record, and a text (TXT) records. TXT record contains additional data needed to resolve or use the service, although it s also often empty.

For a concrete example, consider a hypothetical device that shares music over a local network - an IP - enabled jukebox. Suppose that its transport protocol is TCP and its application protocol goes by the name `music`. When someone plugs the device into an Ethernet hub, a number of things happen, as show in figure 1.

In step 1, the device randomly selects the link-local IP address `169.254.150.84`, randomly selected from IPv4 link-local range `169.254.0.0` with a subnet mask of `255.255.0.0`, and announces it to the network. Because no devices respond to the announcement, the device takes the address as its own.

In step 2, it starts up its own Multicast DNS responder, requests the host name `eds-musicbox.local.`, verifies its availability, and takes the name as its own.

In step 3, the device starts up a music sharing service on TCP port `1010`.

In step 4, finally, it publishes the service, of type `_music._tcp`, under the name `Ed’s Party Mix`, in the `local.` domain, first making sure that no service exists under the same name. This creates two records:

  * An SRV record named `_Ed's Party Mix._music._tcp.local.` that points to `eds-musicbox.local.` on TCP port `1010`.
  * An PTR record named `_music._tcp.local.` that points to the `_Ed's Party Mix._music._tcp.local.` service.

**Figure 1.** Publishing a music sharing service.

---


![https://developer.apple.com/library/mac/documentation/Cocoa/Conceptual/NetServices/Art/rendarch_01publish_2x.png](https://developer.apple.com/library/mac/documentation/Cocoa/Conceptual/NetServices/Art/rendarch_01publish_2x.png)

## Discovery ##

Service discovery makes use of the DNS records registered during service publication to find all named instances of a particular type of service. To do this, an application performs a query for PTR records matching a service type, such as `_http._tcp`.

The Multicast DNS responders running on each device return PTR records with service instance names.

**Figure 2.** Discovering music sharing services.

---


![https://developer.apple.com/library/mac/documentation/Cocoa/Conceptual/NetServices/Art/rendarch_02discover_2x.png](https://developer.apple.com/library/mac/documentation/Cocoa/Conceptual/NetServices/Art/rendarch_02discover_2x.png)

Figure 2 illustrates a client application browsing for music sharing services. In step 1, the client application issues a query for services of type `_music._tcp` in the `local.` domain to the standard multicast address `224.0.0.251`. Every Multicast DNS responder on the network hears the request, but only the music sharing device responds with a PRT record (in step 2).  The resulting PTR record holds the service instance name `Ed's Party Mix._music._tcp.local.` in this case. The client app can then extract service instance name from PTR record and add it to an onscreen list of music servers.

## Resolution ##

Service discovery typically takes place only once in a while - for example, when a user first selects a printer. This operation saves the service instance name, the intended stable identifier for any given instance of a service. Port numbers, IP addresses, and even host names can change from day to day, but a user should not need to reselect a printer every time this happens. Accordingly, resolution from a service name to socket information does not happen until the service is actually used.

To resolve a service, an application performs a DNS lookup for a SRV record with the name of the service. The Multicast DNS responder responds with the SRV record containing the current information.

Figure 3 illustrates service resolution in the music sharing example. The resolution process with a DNS query to the multicast address `224.0.0.251` asking for the `Ed's Party Mix.music._tcp.local.` SRV record (step 1). In step 2, this query returns the service's host name and port number (eds-musicbox.local., 1010). In step 3, the client sends out a multicast request for the IP address. In step 4, this request resolves to the IP address `169.254.150.84`. Then the client can use the IP address and port number to connect to the service. This process takes place each time the service is used, thereby always finding the service’s most current address and port number.

**Figure 3.** Resolving a music sharing service instance.

---


![https://developer.apple.com/library/mac/documentation/Cocoa/Conceptual/NetServices/Art/rendarch_03resolve_2x.png](https://developer.apple.com/library/mac/documentation/Cocoa/Conceptual/NetServices/Art/rendarch_03resolve_2x.png)


## Messaging ##

Now, consider that `Ed's Party Mix.music._tcp.local.` is plugged into the ethernet hub, and that your applications successfully discovered and resolved `Ed's Party Mix.music._tcp.local.`.

Now its time to exchange messages by using a reliable full-duplex network protocol. In DTalk we choose the WebSocket protocol [RFC 6455](http://tools.ietf.org/html/rfc6455).

The WebSocket protocol It is a very thin layer over TCP that transforms a stream of bytes into a stream of messages (either text or binary) and not much more. The WebSocket does imply a messaging architecture but does not mandate the use of any specific messaging protocol.

DTalk provides its own [messaging protocol](MessagingProtocol.md) over WebSocket that is similar to JSON-RPC.

# See Also #

  1. http://www.xmpp.org/extensions/xep-0174.html