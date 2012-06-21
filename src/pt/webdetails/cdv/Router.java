/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdv;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.pentaho.platform.api.engine.IParameterProvider;
import pt.webdetails.cdv.scripts.GlobalScope;
import pt.webdetails.cpf.RequestHandler;
import pt.webdetails.cpf.RestRequestHandler;
import pt.webdetails.cpf.SimpleContentGenerator.MimeType;

/**
 *
 * @author pdpi
 */
public class Router implements RestRequestHandler {

    private Map<Key, Callable> javaScriptHandlers;
    private Map<Key, RequestHandler> javaHandlers;
    private static Router _instance;



    public static synchronized Router getBaseRouter() {
        if (_instance == null) {
            _instance = new Router();
        }
        return _instance;
    }

    public static synchronized Router resetBaseRouter() {
        _instance = new Router();
        return _instance;
    }

    public Router() {
        javaScriptHandlers = new HashMap<Key, Callable>();
        javaHandlers = new HashMap<Key, RequestHandler>();
    }

//    @Override
    public void call(OutputStream out, IParameterProvider pathParams, IParameterProvider requestParams) {
        route(HttpMethod.GET, "", out, pathParams, requestParams);
    }
    
    @Override
    public boolean canHandle(HttpMethod method, String path) {
      Key key = new Key(method, path);
      return javaScriptHandlers.containsKey(key) || javaHandlers.containsKey(key);
    }

    @Override
    public void route(HttpMethod method, String path, OutputStream out, IParameterProvider pathParams, IParameterProvider requestParams) {
        Key key = new Key(method, path);
        try {
            if (javaScriptHandlers.containsKey(key)) {
                Callable handler = javaScriptHandlers.get(key);
                Context cx = GlobalScope.getContextFactory().enterContext();
                try {
                    GlobalScope scope = GlobalScope.getInstance();
                    ResponseWrapper r = new ResponseWrapper((HttpServletResponse) pathParams.getParameter("httpresponse"));
                    Scriptable thiz = cx.getWrapFactory().wrapAsJavaObject(cx, scope, r, null),
                            pParams = cx.getWrapFactory().wrapAsJavaObject(cx, scope, pathParams, null),
                            rParams = cx.getWrapFactory().wrapAsJavaObject(cx, scope, requestParams, null);

                    Object[] params = {out, pParams, rParams};
                    handler.call(cx, scope, thiz, params).toString().getBytes("utf-8");
                } finally {
                    Context.exit();
                }
            } else if (javaHandlers.containsKey(key)) {
                RequestHandler handler = javaHandlers.get(key);
                handler.call(out, pathParams, requestParams);
            }
        } catch (IOException e) {
        }
    }

    public void registerHandler(HttpMethod method, String path, Callable handler) {
        javaScriptHandlers.put(new Key(method, path), handler);
    }

    public void registerHandler(HttpMethod method, String path, RequestHandler handler) {
        javaHandlers.put(new Key(method, path), handler);
    }

    class Key {

        private String path;
        private HttpMethod method;

        public Key(HttpMethod method, String path) {
            this.method = method;
            this.path = path;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Key other = (Key) obj;
            if ((this.method == null) ? (other.method != null) : !this.method.equals(other.method)) {
                return false;
            }
            if ((this.path == null) ? (other.path != null) : !this.path.equals(other.path)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 83 * hash + (this.method != null ? this.method.hashCode() : 0);
            hash = 83 * hash + (this.path != null ? this.path.hashCode() : 0);
            return hash;
        }
    }

    @Override
    public String getResponseMimeType() {
      return MimeType.HTML;
    }
}
