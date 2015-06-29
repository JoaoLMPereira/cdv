/*!
* Copyright 2002 - 2015 Webdetails, a Pentaho company. All rights reserved.
*
* This software was developed by Webdetails and is provided under the terms
* of the Mozilla Public License, Version 2.0, or any later version. You may not use
* this file except in compliance with the license. If you need a copy of the license,
* please go to http://mozilla.org/MPL/2.0/. The Initial Developer is Webdetails.
*
* Software distributed under the Mozilla Public License is distributed on an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. Please refer to
* the license for the specific language governing your rights and limitations.
*/

package pt.webdetails.cdv;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import pt.webdetails.cpf.bean.IBeanFactory;
import pt.webdetails.cpf.PentahoPluginEnvironment;
import pt.webdetails.cpf.exceptions.InitializationException;

public class CdvPentahoEnvironment extends PentahoPluginEnvironment implements ICdvEnvironment {

  protected static Log logger = LogFactory.getLog( CdvPentahoEnvironment.class );

  private static final String PLUGIN_REPOSITORY_DIR = "cdv";
  private static final String SYSTEM_DIR = "system";
  private static final String PLUGIN_ID = "cdv";
  private static final String JS_DIR = "js/";
  private static final String TESTS_DIR = "cdv/tests/";

  private IBeanFactory factory;

  public void init( IBeanFactory factory ) throws InitializationException {
    this.factory = factory;
    init( this );
  }

  public void refresh() {
    try {
      init( this.factory );
    } catch ( InitializationException e ) {
      logger.error( "CdvPentahoEnvironment.refresh()", e );
    }
  }

  @Override
  public String getPluginRepositoryDir() {
    return PLUGIN_REPOSITORY_DIR;
  }

  @Override
  public String getSystemDir() {
    return SYSTEM_DIR;
  }

  @Override
  public String getPluginId() {
    return PLUGIN_ID;
  }

  @Override
  public String getJsDir() {
    return JS_DIR;
  }

  @Override
  public String getTestsDir() {
    return TESTS_DIR;
  }

  @Override
  public void ensureDefaultDirAndFilesExists() {
    CdvEngine.getInstance().ensureBasicDirsAndFiles();
  }
}
