/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.webdetails.cdv;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.springframework.security.wrapper.SavedRequestAwareWrapper;
import pt.webdetails.cdv.Router.HttpMethod;
import pt.webdetails.cpf.InterPluginCall;
import pt.webdetails.cpf.SimpleContentGenerator;

/**
 *
 * @author pdpi
 */
public class CdvContentGenerator extends SimpleContentGenerator {//BaseContentGenerator {

    private static final long serialVersionUID = 1L;
    public static final String CDW_EXTENSION = ".cdw";
    public static final String PLUGIN_NAME = "cdv";
    public static final String PLUGIN_PATH = "system/" + CdvContentGenerator.PLUGIN_NAME + "/";

    @Override
    public Log getLogger() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public String getPluginName() {
      return PLUGIN_NAME;
    }

    @Override
    public void createContent() {//throws Exception {

      try{
      
        final IParameterProvider requestParams = parameterProviders.get(IParameterProvider.SCOPE_REQUEST);
        final IParameterProvider pathParams = parameterProviders.get("path");
        OutputStream out = outputHandler.getOutputContentItem("response", "content", "", instanceId, MimeType.HTML).getOutputStream(null);
        final String path = pathParams.getStringParameter("path", null);

        SavedRequestAwareWrapper wrapper = (SavedRequestAwareWrapper) pathParams.getParameter("httprequest");
        HttpMethod method = HttpMethod.valueOf(wrapper.getMethod());
        if ("/refresh".equals(path)) {
            refresh(out, pathParams, requestParams);
        } else if ("/home".equals(path)) {
            home(out, pathParams, requestParams);
        } else if ("/tests".equals(path)) {
            validations(out, pathParams, requestParams);
        } else if ("/cdaErrors".equals(path)) {
            cdaErrors(out, pathParams, requestParams);
        } else if ("/slowQueries".equals(path)) {
            slowQueries(out, pathParams, requestParams);
        } else {
            Router.getBaseRouter().route(method, path, out, pathParams, requestParams);
        }
      } catch(Exception e){
        logger.error(e);
      }
    }

    private void refresh(OutputStream out, IParameterProvider pathParams, IParameterProvider requestParams) {
        CdvLifecycleListener.reInit();
    }

    private void home(OutputStream out, IParameterProvider pathParams, IParameterProvider requestParams) throws UnsupportedEncodingException, IOException {
        callCDE("home.wcdf", out, pathParams, requestParams);
    }

    private void validations(OutputStream out, IParameterProvider pathParams, IParameterProvider requestParams) throws UnsupportedEncodingException, IOException {
        callCDE("validations.wcdf", out, pathParams, requestParams);
    }

    private void cdaErrors(OutputStream out, IParameterProvider pathParams, IParameterProvider requestParams) throws UnsupportedEncodingException, IOException {
        callCDE("cdaErrors.wcdf", out, pathParams, requestParams);
    }

    private void slowQueries(OutputStream out, IParameterProvider pathParams, IParameterProvider requestParams) throws UnsupportedEncodingException, IOException {
        callCDE("slowQueries.wcdf", out, pathParams, requestParams);
    }

    private void callCDE(String file, OutputStream out, IParameterProvider pathParams, IParameterProvider requestParams) throws UnsupportedEncodingException, IOException {


        ServletRequestWrapper wrapper = (ServletRequestWrapper) pathParams.getParameter("httprequest");
        String root = wrapper.getServerName() + ":" + wrapper.getServerPort();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("solution", "system");
        params.put("path", "cdv/presentation/");
        params.put("file", file);
        params.put("absolute", "true");
        params.put("root", root);
        parseParameters(params);

        if (requestParams.hasParameter("mode") && requestParams.getStringParameter("mode", "Render").equals("edit")) {
            redirectToCDE(out, params);
            return;
        }


        InterPluginCall pluginCall = new InterPluginCall(InterPluginCall.CDE, "Render", params);
        pluginCall.setResponse(getResponse());
        pluginCall.setOutputStream(out);
        pluginCall.run();

    }

    private void parseParameters(Map<String, Object> params) {
        //add request parameters
        ServletRequest request = getRequest();
        @SuppressWarnings("unchecked")//should always be String
        Enumeration<String> originalParams = request.getParameterNames();
        // Iterate and put the values there
        while (originalParams.hasMoreElements()) {
            String originalParam = originalParams.nextElement();
            params.put(originalParam, request.getParameter(originalParam));
        }
    }

    private void redirectToCDE(OutputStream out, Map<String, Object> params) throws UnsupportedEncodingException, IOException {

        //TODO: use proper redirect
      
        StringBuilder str = new StringBuilder();
        str.append("<html><head><title>Redirecting</title>");
        str.append("<meta http-equiv=\"REFRESH\" content=\"0; url=../pentaho-cdf-dd/edit?");

        List<String> paramArray = new ArrayList<String>();
        for(String key : params.keySet()){
          Object value = params.get(key);
          if (value instanceof String) {
              paramArray.add(key + "=" + URLEncoder.encode((String) value, "UTF-8"));
          }
        }

        str.append(StringUtils.join(paramArray, "&"));
        str.append("\"></head>");
        str.append("<body>Redirecting</body></html>");

        out.write(str.toString().getBytes("UTF-8"));
        return;

    }
}
