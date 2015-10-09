package com.dsvdsv.gradle.plugin.eclipse.jetty

import groovy.util.logging.Slf4j
import org.apache.tools.ant.AntClassLoader
import org.gradle.api.UncheckedIOException
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.TaskAction

/**
 * User: Dikansky
 * Date: 19.12.2014
 */
@Slf4j
abstract class BaseJettyTask extends ConventionTask {
	boolean reloadable;
	FileCollection buildscriptClasspath;
	FileCollection jettyClasspath;
	protected Thread consoleScanner;

	JettyServer server;

	protected abstract void validateConfiguration();

	protected abstract void startJettyInternal();

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

	protected URLClassLoader createJettyClassLoader() {
		ClassLoader rootClassLoader = new AntClassLoader(getClass().classLoader, false);
		URLClassLoader pluginClassLoader = new URLClassLoader(toURLArray(getBuildscriptClasspath().files), rootClassLoader);
		new URLClassLoader(toURLArray(getJettyClasspath().files), pluginClassLoader);
	}

	protected URL[] toURLArray(Collection<File> files) {
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


	protected void startConsoleScanner() throws Exception {
		this.startConsoleScanner {{ reconfigureScanner -> restartWebApp(reconfigureScanner) }}
	}

	protected void startConsoleScanner(Closure reload) throws Exception {
		if (reloadable) {
			log.info('Console reloading is ENABLED. Hit ENTER on the console to restart the context.');
			consoleScanner = new ConsoleScanner(reload);
			consoleScanner.start();
		}
	}

	protected void restartWebApp(boolean reconfigureScanner) {
		log.info 'Restart jetty';

		log.info 'Stopping jetty....';
		server.contexts.each { x -> x.stop() }
		log.info 'Jetty stopped';
		log.info 'Starting jetty....';

		server.contexts.each { x -> x.start() }
	}

	protected JettyServer createServer() {
		JettyFactory.instance.jettyServer
	}
}
