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
package com.arkasoft.freddo.dtalk.j2ee.server;

class DTalkConnectionEvent {

  private final DTalkServerEndpoint conn;
  private final boolean open;

  public DTalkConnectionEvent(DTalkServerEndpoint conn, boolean open) {
    this.conn = conn;
    this.open = open;
  }

  public DTalkServerEndpoint getConnection() {
    return conn;
  }

  public boolean isOpen() {
    return open;
  }

}
