package com.dotmarketing.osgi.scripting.support;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;

import com.caucho.quercus.Quercus;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.osgi.CombinedLoader;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

/**
 * This class provides methods that make it easier to
 * use the BSF Within dotCMS Scripting Plugin.
 *
 * @author Jason Tesser
 */
public class BSFUtil {

	private static final int COLUMN_NO = -1;
	private static final int LINE_NO = -1;

	private ThreadLocal<BSFManager> manager = new ThreadLocal<BSFManager>(){
		protected BSFManager initialValue() {
            return initManager();
        }

	};
	private static BSFUtil instance;
	private static String realPath = null;
	private static String assetPath = "/assets";

	private BSFUtil() {
		try {
			realPath = Config.getStringProperty("ASSET_REAL_PATH");
		} catch (Exception e) { }
		try {
			assetPath = Config.getStringProperty("ASSET_PATH");
		} catch (Exception e) { }
	}

	private BSFManager initManager(){
		return new BSFManager();
	}

	private synchronized static void init(){
		if(instance != null)
			return;
		instance = new BSFUtil();
	}

	public static BSFUtil getInstance(){
		if(instance == null){
			init();
		}
		return instance;
	}

	/**
	 * Lookup a variable from the script
	 * @param variableName
	 * @return
	 */
	public Object lookupBean(String variableName){
		return manager.get().lookupBean(variableName);
	}

	/**
	 * Declares a global variable that the scripting language can use to access a given Java object.
	 * @param name
	 * @param bean
	 * @throws BSFException
	 */
	public void declareBean(String name, Object bean) throws BSFException {
		manager.get().declareBean(name, bean, bean.getClass());
	}

	/**
	 * Evaluates an expression on a file and returns its value.
	 *
	 * Important:
	 * Based on known classloading issues on OSGi the "eval"
	 * methods implements an strategy to force the manager thread
	 * to use a known classloader instead of allowing it to attempt
	 * to use a new one, this ensures that the manager classloader
	 * finds the php engine class.  This pattern will be followed by
	 * the next "eval" methods.
	 * @param fileKey - This can be a
	 * @param expression
	 * @return
	 * @throws BSFException
	 */
	public Object evalExpression(String language, String expression) throws BSFException, Exception {
		ClassLoader oldLoader = manager.get().getClassLoader();
		ClassLoader newLoader = this.getClass().getClassLoader();
		CombinedLoader loader = new CombinedLoader(newLoader);
    	loader.addLoader(Quercus.class.getClassLoader());
    	Thread.currentThread().setContextClassLoader( loader );
		manager.get().setClassLoader(newLoader);
		manager.get().loadScriptingEngine(language);

		String source = "(java)";
		Object value = manager.get().eval(language, source, LINE_NO, COLUMN_NO, expression);

		manager.get().setClassLoader(oldLoader);
		return value;
	}

	/**
	 * Evaluates an expression on a file and returns its value.
	 * @param filePath
	 * @param expression
	 * @return
	 * @throws BSFException
	 * @throws DotSecurityException
	 * @throws IOException
	 * @throws DotDataException
	 * @throws NoSuchUserException
	 */
	public Object evalExpressionOnFile(String filePath, Host host, String expression) throws BSFException, IOException, DotSecurityException, DotDataException {
		//		String source = readFile(filePath, host);
		String language = whichLanguage(filePath);
		return manager.get().eval(language, buildFilePath(filePath, host), LINE_NO, COLUMN_NO, expression);
	}

	/**
	 * Evaluates an expression on passed in scriplet
	 * @param language
	 * @param scriplet
	 * @param expression
	 * @return
	 * @throws BSFException
	 */
	public Object evalExpressionOnScriptlet(String language, String scriplet, String expression) throws BSFException {
		return manager.get().eval(language, scriplet, LINE_NO, COLUMN_NO, expression);
	}

	/**
	 * Evaluates a file of code and returns the value of the last expression.
	 * @param filePath
	 * @return
	 * @throws BSFException
	 * @throws IOException
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws NoSuchUserException
	 */
	public Object evalFile(String filePath, Host host) throws BSFException, IOException, DotSecurityException, DotDataException {
		String language = whichLanguage(filePath);
		return manager.get().eval(language, buildFilePath(filePath, host), LINE_NO, COLUMN_NO, "");
	}

	/**
	 * Find the language being used based on the file extension
	 * @param filePath
	 * @return
	 * @throws BSFException
	 */
	public String whichLanguage(String filePath) throws BSFException{
		return BSFManager.getLangFromFilename(filePath);
	}

	private String buildFilePath(String filePath, Host host){
		return host.getHostname() + "DOTHOST" + filePath;
	}

	/**
	 * Reads the entire content of a given file into a String and returns it.
	 * @param filePath - dotCMS file path
	 * @return
	 * @throws IOException
	 * @throws DotDataException
	 * @throws NoSuchUserException
	 */
	public static String readFile(String filePath, Host host) throws IOException,DotSecurityException, NoSuchUserException, DotDataException {
		Identifier ident = APILocator.getIdentifierAPI().find(host, filePath);
		String uri = LiveCache.getPathFromCache(ident.getURI(), host);

		String inode = UtilMethods.getFileName(new File(FileUtil.getRealPath(assetPath + uri)).getName());
		com.dotmarketing.portlets.files.model.File file = APILocator.getFileAPI().find(inode, APILocator.getUserAPI().getSystemUser(), false);

		if(!Config.getBooleanProperty("ENABLE_SCRIPTING", false)){
			throw new DotSecurityException("Last Mod User does not have Scripting Developer role");
		}
		User mu = APILocator.getUserAPI().loadUserById(file.getModUser(), APILocator.getUserAPI().getSystemUser(), true);
		if(!APILocator.getRoleAPI().doesUserHaveRole(mu, APILocator.getRoleAPI().loadRoleByKey("Scripting Developer"))){
			throw new DotSecurityException("Last Mod User does not have Scripting Developer role");
		}

		FileReader fr = null;
		if(!UtilMethods.isSet(realPath)){
			fr = new FileReader(FileUtil.getRealPath(assetPath + uri));
		}else{
			fr = new FileReader(realPath + uri);
		}
		BufferedReader br = new BufferedReader(fr);
		String content = "";

		while (true) {
			String line = br.readLine();
			if (line == null) break;
			content += line + '\n';
		}

		return content;
	}

	public void terminateThreadLocalManager(){
		BSFManager man = manager.get();
		if(man != null){
			man.terminate();
			man=null;
		}
	}

	public void registerScriptingEngine(String language, String engine, String[] extensions){
		BSFManager.registerScriptingEngine(language, engine, extensions);
	}
}