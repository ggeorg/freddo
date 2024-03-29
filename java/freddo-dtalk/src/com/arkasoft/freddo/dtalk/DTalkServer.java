/*
 * Copyright 2013-2014 ArkaSoft LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.arkasoft.freddo.dtalk;

import java.net.InetSocketAddress;

public interface DTalkServer {
	
	static final String DTALKSRV_PATH = "/dtalksrv";
  
  void startServer() throws Exception;

  void startServer(int port) throws Exception;;

  void startServer(InetSocketAddress socketAddress) throws Exception;

  void stopServer() throws Exception;

  InetSocketAddress getSocketAddress();

}
