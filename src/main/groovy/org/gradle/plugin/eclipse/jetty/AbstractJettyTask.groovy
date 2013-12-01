package org.gradle.plugin.eclipse.jetty

import groovy.util.logging.Slf4j
import org.apache.tools.ant.AntClassLoader
import org.gradle.api.GradleException
import org.gradle.api.UncheckedIOException
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.logging.ProgressLogger
import org.gradle.logging.ProgressLoggerFactory

/**
 * User: dsv
 * Date: 25.08.13
 * Time: 19:48
 */
@Slf4j
abstract class AbstractJettyTask extends ConventionTask {
	@InputFiles
	Iterable<File> additionalRuntimeJars = new ArrayList<File>();
	JettyServer server;
	def webAppConfig;
	String contextPath;
	@InputFile
	@Optional
	File jettyEnvXml;
	@InputFile
	@Optional
	File webDefaultXml;
	@InputFile
	@Optional
	File overrideWebXml;
	int scanIntervalSeconds;
	boolean reloadable;
	File jettyConfig;
	Integer stopPort;
	String stopKey;
	FileCollection buildscriptClasspath;
	FileCollection jettyClasspath;
	boolean daemon;
	Integer httpPort;
	def loginServices
	def requestLog;

	protected Scanner scanner;
	protected ArrayList<File> scanList;
	protected Thread consoleScanner;


	static final String PORT_SYSPROPERTY = "jetty.port";

	public abstract void validateConfiguration();

	@TaskAction
	protected void start() {
		logger.info("Configuring Jetty for " + getProject());
		ClassLoader originClassLoader = getClass().classLoader;
		URLClassLoader jettyClassLoader = createJettyClassLoader();

		try {
			Thread.currentThread().contextClassLoader = jettyClassLoader;
			validateConfiguration();
			startJettyInternal();
		}
		finally {
			Thread.currentThread().contextClassLoader = originClassLoader;
		}
	}

	URLClassLoader createJettyClassLoader() {
		ClassLoader rootClassLoader = new AntClassLoader(getClass().classLoader, false);
		URLClassLoader pluginClassLoader = new URLClassLoader(toURLArray(getBuildscriptClasspath().files), rootClassLoader);
		new URLClassLoader(toURLArray(getJettyClasspath().files), pluginClassLoader);
	}

	private URL[] toURLArray(Collection<File> files) {
		List<URL> urls = new ArrayList<URL>(files.size())

		for (File file : files) {
			try {
				urls.add(file.toURI().toURL())
			}
			catch (MalformedURLException e) {
				throw new UncheckedIOException(e)
			}
		}

		urls.toArray(new URL[urls.size()]);
	}

	def startJettyInternal() {
		ProgressLoggerFactory progressLoggerFactory = getServices().get(ProgressLoggerFactory.class);
		ProgressLogger progressLogger = progressLoggerFactory.newOperation(AbstractJettyTask.class);
		progressLogger.setDescription("Start Jetty server");
		progressLogger.setShortDescription("Starting Jetty");
		progressLogger.started();
		try {
			setServer(createServer());

			JettyServer server = getServer();
			getServer().configureConnector(getHttpPort());

			//set up the webapp and any context provided
			server.configureHandlers();
			configureWebApplication();
			server.addWebApplication(webAppConfig);

			//do any other configuration required by the
			//particular Jetty version
			if (getStopPort() != null && getStopPort() > 0 && getStopKey() != null) {
				server.enableShutdownMonitor(getStopPort(), getStopKey(), isDaemon());
			}

			// start Jetty
			server.start();

			if (isDaemon()) {
				return;
			}

			// start the new line scanner thread if necessary
			startConsoleScanner();

		} catch (Exception e) {
			throw new GradleException("Could not start the Jetty server.", e);
		} finally {
			progressLogger.completed();
		}

		progressLogger = progressLoggerFactory.newOperation(this.getClass());
		progressLogger.setDescription(String.format("Run Jetty at http://localhost:%d/%s", getHttpPort(), getContextPath()));
		progressLogger.setShortDescription(String.format("Running at http://localhost:%d/%s", getHttpPort(), getContextPath()));
		progressLogger.started();
		try {
			// keep the thread going if not in daemon mode
			server.join();
		} catch (Exception e) {
			throw new GradleException("Failed to wait for the Jetty server to stop.", e);
		} finally {
			progressLogger.completed();
		}
	}

	void configureWebApplication() {
		if (webAppConfig == null) {
			webAppConfig = new JettyWebAppContext();
		}
		webAppConfig.setContextPath(getContextPath().startsWith("/") ? getContextPath() : "/" + getContextPath());
		if (getTemporaryDir() != null) {
			webAppConfig.setTempDirectory(getTemporaryDir());
		}
		if (getWebDefaultXml() != null) {
			webAppConfig.setDefaultsDescriptor(getWebDefaultXml().getCanonicalPath());
		}
		if (getOverrideWebXml() != null) {
			webAppConfig.setOverrideDescriptor(getOverrideWebXml().getCanonicalPath());
		}
		if (getJettyEnvXml() != null) {
			webAppConfig.setJettyEnvXmlFile(getJettyEnvXml());
		}
		Set<String> systemClasses = new LinkedHashSet<String>(Arrays.asList(webAppConfig.getSystemClasses()));
		systemClasses.remove('org.apache.commons.logging.');
		systemClasses.remove('org.apache.log4j.');
		webAppConfig.setSystemClasses(systemClasses.toArray(new String[systemClasses.size()]));

		webAppConfig.setParentLoaderPriority(false);

		logger.info "Context path = ${webAppConfig.getContextPath()}"
		logger.info 'Tmp directory =  determined at runtime'
		logger.info "Web defaults = " + (webAppConfig.getDefaultsDescriptor() == null ? " jetty default" : webAppConfig.getDefaultsDescriptor());
		logger.info "Web overrides = " + (webAppConfig.getOverrideDescriptor() == null ? " none" : webAppConfig.getOverrideDescriptor());
	}

	def createServer() {
		JettyFactory.instance.jettyServer
	}

	/**
	 * Run a thread that monitors the console input to detect ENTER hits.
	 */
	void startConsoleScanner() throws Exception {
		if (reloadable) {
			log.info('Console reloading is ENABLED. Hit ENTER on the console to restart the context.');
			consoleScanner = new ConsoleScanner(this);
			consoleScanner.start();
		}
	}

	void restartWebApp(boolean reconfigureScanner) {
		log.info 'Restart jetty';

		log.info 'Stopping jetty....';
		webAppConfig.stop();
		log.info 'Jetty stopped';
		validateConfiguration();
		log.info 'Starting jetty....';

		webAppConfig.start();
	}
}
