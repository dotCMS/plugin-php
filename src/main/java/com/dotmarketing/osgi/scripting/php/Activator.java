package com.dotmarketing.osgi.scripting.php;

/**
 * Activator
 *
 * Registers scripting engine
 * Registers viewtool ($php)
 * Registers servlet code to handle script assets
 * ---
 * The location for the servlet used for script file assets evaluation name
 * will be formed like this:
 *
 * <host>/app/SERVLET_NAME
 *
 * Usage
 * <host>/app/SERVLER_NAME/folder1/folder2/script.php
 *
 * Note:
 * Most of the code was migrated from the core branch on dotCMS versions prior to 3.2.x
 *
 *
 * There is support code from the core library being used like:
 * GenericBundleActivator
 * ScriptingTool
 *
 * Also AbstractScriptingTool was copied over to reference this bundle's BSFUtil code.
 */

import java.util.Dictionary;
import java.util.Hashtable;

import com.dotcms.repackage.org.apache.felix.http.api.ExtHttpService;
import com.dotcms.repackage.org.osgi.framework.BundleContext;
import com.dotcms.repackage.org.osgi.framework.ServiceReference;
import com.dotcms.repackage.org.osgi.service.http.HttpContext;
import com.dotcms.repackage.org.osgi.util.tracker.ServiceTracker;
import com.dotmarketing.osgi.GenericBundleActivator;
import com.dotmarketing.osgi.scripting.php.engine.PHPEngine;
import com.dotmarketing.osgi.scripting.php.servlets.PHPServlet;
import com.dotmarketing.osgi.scripting.php.viewtool.PHPToolInfo;
import com.dotmarketing.osgi.scripting.support.BSFUtil;

public class Activator extends GenericBundleActivator {

	private static final String LANGUAGE = "php";
	private static final String SERVLET_NAME = "phpservlet";
	private static final String PARAM_COMPILE = "compile";

    private PHPServlet phpServlet;
    private ExtHttpService httpService;
    private ServiceTracker phpServeltServiceTracker;

    @Override
    public void start ( BundleContext bundleContext ) throws Exception {

    	initializeServices( bundleContext );

    	// uses the Bean Scripting Framework to register "resin.jar" as PHP engine
    	BSFUtil.getInstance().registerScriptingEngine(LANGUAGE, PHPEngine.class.getName(), new String[]{"php"});

    	// registers view tool ($php)
        registerViewToolService( bundleContext, new PHPToolInfo() );

        //Create new ServiceTracker
        phpServeltServiceTracker = new ServiceTracker( bundleContext, PHPServlet.class.getName(), null );
        ServiceReference sRef = bundleContext.getServiceReference( ExtHttpService.class.getName() );

        if ( sRef != null ) {

        	phpServeltServiceTracker.addingService( sRef );
            httpService = (ExtHttpService) bundleContext.getService( sRef );
            try {
                // Registering a servlet
            	Dictionary<String,String> dict=new Hashtable<String,String>();
                dict.put(PARAM_COMPILE,"true");
                HttpContext ctx = httpService.createDefaultHttpContext();
                // servlet to process script files.
            	phpServlet = new PHPServlet( phpServeltServiceTracker );
                httpService.registerServlet( "/"+SERVLET_NAME, phpServlet, dict, ctx );

            } catch ( Exception e ) {
                e.printStackTrace();
            }
        }

        // open service tracker to start tracking
        phpServeltServiceTracker.open();
    }

    @Override
    public void stop ( BundleContext bundleContext ) throws Exception {
    	// unregister view tool ($php)
        unregisterViewToolServices();

        // if the servlet is active
        if ( httpService != null && phpServlet != null ) {
            httpService.unregisterServlet( phpServlet );
        }

        // when the bundle is closed the thread running the BSFManager is killed
        BSFUtil.getInstance().terminateThreadLocalManager();
    }
}
