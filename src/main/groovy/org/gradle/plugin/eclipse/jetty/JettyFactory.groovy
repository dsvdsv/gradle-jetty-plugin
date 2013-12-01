package org.gradle.plugin.eclipse.jetty

import groovy.util.logging.Slf4j
import org.gradle.api.GradleException

/**
 * User: Dikansky
 * Date: 21.10.13
 */
@Slf4j
@Singleton
class JettyFactory {

	JettyServer getJettyServer() {
		ClassLoader classLoader = Thread.currentThread().contextClassLoader;
		try {
			log.warn 'Resolved Jetty server implementation in webAppClasses'
			return new JettyServerImpl(loadClass('org.eclipse.jetty.server.Server').newInstance());
		} catch (ClassNotFoundException ex) {
			throw new GradleException('Unable to find embedded Jetty server implementation in webAppClasses.')
		}
		return null;
	}

	def loadClass(String clazz) {
		ClassLoader classLoader = Thread.currentThread().contextClassLoader;
		classLoader.loadClass(clazz)
	}
}
