package org.gradle.plugin.eclipse.jetty

import groovy.util.logging.Slf4j
import org.apache.tools.ant.AntClassLoader
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.UncheckedIOException
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.tasks.TaskAction
import org.gradle.logging.ProgressLogger
import org.gradle.logging.ProgressLoggerFactory

/**
 * User: Dikansky
 * Date: 19.12.2014
 */
@Slf4j
class FarmJettyRunTask extends ConventionTask {
	Integer stopPort;
	String stopKey;
	boolean daemon;
	Integer httpPort;
	boolean reloadable;

	JettyServer server;
	protected Thread consoleScanner;

	FileCollection buildscriptClasspath;
	FileCollection jettyClasspath;

	List<JettyWebAppContext> collectWebAppContexts() {
		List<JettyWebAppContext> result = new ArrayList<>();
		project.childProjects.each {String name, Project p ->
			if (p.plugins.hasPlugin(WarPlugin) && p.plugins.hasPlugin(JettyPlugin))  {
				JettyRunTask jettyRunTask = p.tasks.jettyRun;
				jettyRunTask.validateConfiguration();
				jettyRunTask.configureWebApplication();
				result.add(jettyRunTask.webAppConfig)
			}
		}
		return result;
	}

	@TaskAction
	void start() {
		logger.info("Configuring Jetty for " + getProject());
		ClassLoader originClassLoader = getClass().classLoader;
		URLClassLoader jettyClassLoader = createJettyClassLoader();

		try {
			Thread.currentThread().contextClassLoader = jettyClassLoader;
			List<JettyWebAppContext> contexts = collectWebAppContexts();
			startJettyInternal(contexts);
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

	void startJettyInternal(List<JettyWebAppContext> contexts) {
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
			contexts.each {x-> server.addWebApplication(x)}

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
		def contextPaths = '';
		contexts.each {x->contextPaths+ ' ' + x.getContextPath()}
		progressLogger = progressLoggerFactory.newOperation(this.getClass());
		progressLogger.setDescription(String.format("Run Jetty at http://localhost:%d/%s", getHttpPort(), contextPaths));
		progressLogger.setShortDescription(String.format("Running at http://localhost:%d/%s", getHttpPort(), contextPaths));
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
