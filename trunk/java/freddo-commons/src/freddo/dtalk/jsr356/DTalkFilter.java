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
package freddo.dtalk.jsr356;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import freddo.dtalk.util.LOG;

public class DTalkFilter implements Filter {
  private static final String TAG = LOG.tag(DTalkFilter.class);

  private FilterConfig filterConfig = null;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    LOG.v(TAG, ">>> init: %s", filterConfig);
    this.filterConfig = filterConfig;
  }

  @Override
  public void destroy() {
    LOG.v(TAG, ">>> destroy");
    filterConfig = null;
  }

  @Override
  public void doFilter(final ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    LOG.v(TAG, ">>> doFilter: %s", ((HttpServletRequest) request).getRequestURI());
    
    if (filterConfig == null) {
      return;
    }

    chain.doFilter(new HttpServletRequestWrapper((HttpServletRequest) request) {
      private Map<String, String[]> requestParams = null;

      public Map<String, String[]> getParameterMap() {
        if (requestParams == null) {
          requestParams = new ConcurrentHashMap<String, String[]>(super.getParameterMap());
          requestParams.put("dtalk-remote-address", new String[] {getRemoteAddr()});
          // map.put("dtalk-remote-host", new String[] {getRemoteHost()});
        }
        return requestParams;
      }

    }, response);
  }

}
