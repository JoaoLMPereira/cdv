/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdv;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IPluginLifecycleListener;
import org.pentaho.platform.api.engine.PluginLifecycleException;
import pt.webdetails.cdv.scripts.GlobalScope;
import pt.webdetails.cpf.RestRequestHandler.HttpMethod;

/**
 * This class inits Cdv plugin within the bi-platform
 * @author pdpi
 *
 */
public class CdvLifecycleListener implements IPluginLifecycleListener {

    static Log logger = LogFactory.getLog(CdvLifecycleListener.class);

    public void init() throws PluginLifecycleException {
        reInit();
    }

    public static void reInit() {
        GlobalScope scope = GlobalScope.reset();
        Router.resetBaseRouter().registerHandler(HttpMethod.GET, "/hello", new DummyHandler());
        scope.executeScript("system/cdv/js/bootstrap.js");
    }

    public void loaded() throws PluginLifecycleException {
    }

    public void unLoaded() throws PluginLifecycleException {
    }
}
