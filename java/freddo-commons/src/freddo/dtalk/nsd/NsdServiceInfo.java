package freddo.dtalk.nsd;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Map;

public interface NsdServiceInfo extends Serializable {

	/** Get the service name */
	String getServiceName();

	/** Get the service type */
	String getServiceType();

	/** Get a TxtRecord value for a key */
  Map<String, String> getTxtRecord();

	/** Get the host address. The host address is valid for a resolved service. */
	InetAddress getHost();

	/** Get port number. The port number is valid for a resolved service. */
	int getPort();

}
