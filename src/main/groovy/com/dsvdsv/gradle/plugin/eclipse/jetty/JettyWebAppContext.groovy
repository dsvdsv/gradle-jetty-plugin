package com.dsvdsv.gradle.plugin.eclipse.jetty

import groovy.util.logging.Slf4j
import org.eclipse.jetty.plus.webapp.EnvConfiguration
import org.eclipse.jetty.plus.webapp.PlusConfiguration
import org.eclipse.jetty.servlet.FilterHolder
import org.eclipse.jetty.servlet.FilterMapping
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.servlet.ServletMapping
import org.eclipse.jetty.util.URIUtil
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.webapp.*

/**
 * User: dsv
 * Date: 25.08.13
 * Time: 15:54
 */
@Slf4j
class JettyWebAppContext extends WebAppContext {
	static final String DEFAULT_CONTAINER_INCLUDE_JAR_PATTERN = '.*/javax.servlet-[^/]*\\.jar$|.*/servlet-api-[^/]*\\.jar$';
	static final String WEB_INF_CLASSES_PREFIX = '/WEB-INF/classes';
	static final String WEB_INF_LIB_PREFIX = '/WEB-INF/lib';

	final Set<File> classPathFiles = new HashSet<File>();
	Set<File> webAppClasses;
	File jettyEnvXmlFile;
	File classes = null;
	File testClasses = null;
	String webInfIncludeJarPattern;
	final Set<File> webInfClasses = new HashSet<File>();
	final Set<File> webInfJars = new HashSet<File>();
	List<String> extraResourceBases;

	final EnvConfiguration envConfig;
	final GradleWebInfConfiguration webInfConfiguration;
	public JettyWebAppContext() {
		super();
		configurations = [
				webInfConfiguration = new GradleWebInfConfiguration(),
				new WebXmlConfiguration(),
				new MetaInfConfiguration(),
				new FragmentConfiguration(),
				envConfig = new EnvConfiguration(),
				new PlusConfiguration(),
				new GradleAnnotationConfiguration(),
				new JettyWebXmlConfiguration()
		];
		// Turn off copyWebInf option as it is not applicable for plugin.
		super.setCopyWebInf(false);
	}

	void doStart() throws Exception {
		//Set up the pattern that tells us where the jars are that need scanning for
		//stuff like taglibs so we can tell jasper about it (see TagLibConfiguration)

		//Allow user to set up pattern for names of jars from the container webAppClasses
		//that will be scanned - note that by default NO jars are scanned
		String tmp = (String) getAttribute(WebInfConfiguration.CONTAINER_JAR_PATTERN);
		tmp = addPattern(tmp, DEFAULT_CONTAINER_INCLUDE_JAR_PATTERN);
		setAttribute(WebInfConfiguration.CONTAINER_JAR_PATTERN, tmp);

		classPathFiles.clear();

		//Allow user to set up pattern of jar names from WEB-INF that will be scanned.
		//Note that by default ALL jars considered to be in WEB-INF will be scanned - setting
		//a pattern restricts scanning
		if (webInfIncludeJarPattern != null)
			setAttribute(WebInfConfiguration.WEBINF_JAR_PATTERN, webInfIncludeJarPattern);

		//Set up the classes dirs that comprises the equivalent of WEB-INF/classes
		if (testClasses != null)
			webInfClasses.add(testClasses);
		if (classes != null)
			webInfClasses.add(classes);

		// Set up the webAppClasses
		classPathFiles.clear();
		if (webAppClasses != null) {
			classPathFiles.addAll(webAppClasses);
		}
		classPathFiles.addAll(webInfClasses);
		classPathFiles.addAll(webInfJars);


		if (jettyEnvXmlFile != null) {
			envConfig.setJettyEnvXml(jettyEnvXmlFile.toURI().toURL());
		}

		if(extraResourceBases!=null && !extraResourceBases.empty) {
			webInfConfiguration.setExtraResourceBases(extraResourceBases)
		}
		// CHECK setShutdown(false);
		super.doStart();
	}

	void doStop() throws Exception {
		classPathFiles.clear();

		classes = null;
		testClasses = null;
		webInfClasses.clear();
		webInfJars.clear();

		// CHECK setShutdown(true);
		//just wait a little while to ensure no requests are still being processed
		Thread.currentThread().sleep(500L);
		super.doStop();

		//remove all listeners, servlets and filters. This is because we will re-apply
		//any context xml file, which means they would potentially be added multiple times.
		setEventListeners(new EventListener[0]);
		getServletHandler().setFilters(new FilterHolder[0]);
		getServletHandler().setFilterMappings(new FilterMapping[0]);
		getServletHandler().setServlets(new ServletHolder[0]);
		getServletHandler().setServletMappings(new ServletMapping[0]);
	}


	@Override
	public Resource getResource(String uriInContext) throws MalformedURLException {
		Resource resource = null;
		// Try to get regular resource
		resource = super.getResource(uriInContext);

		// If no regular resource exists check for access to /WEB-INF/lib or /WEB-INF/classes
		if ((resource == null || !resource.exists()) && uriInContext != null && classes != null) {
			String uri = URIUtil.canonicalPath(uriInContext);
			if (uri == null)
				return null;

			try {
				// Replace /WEB-INF/classes with candidates for the webAppClasses
				if (uri.startsWith(WEB_INF_CLASSES_PREFIX)) {
					if (uri.equalsIgnoreCase(WEB_INF_CLASSES_PREFIX) || uri.equalsIgnoreCase(WEB_INF_CLASSES_PREFIX + "/")) {
						//exact match for a WEB-INF/classes, so preferentially return the resource matching the web-inf classes
						//rather than the test classes
						if (classes != null)
							return Resource.newResource(classes);
						else if (testClasses != null)
							return Resource.newResource(testClasses);
					} else {
						//try matching
						Resource res = null;
						int i = 0;
						while (res == null && (i < webInfClasses.size())) {
							String newPath = uri.replace(WEB_INF_CLASSES_PREFIX, webInfClasses.get(i).getPath());
							res = Resource.newResource(newPath);
							if (!res.exists()) {
								res = null;
								i++;
							}
						}
						return res;
					}
				} else if (uri.startsWith(WEB_INF_LIB_PREFIX)) {
					// Return the real jar file for all accesses to
					// /WEB-INF/lib/*.jar
					String jarName = uri.replace(WEB_INF_LIB_PREFIX, '');
					if (jarName.startsWith('/') || jarName.startsWith('\\'))
						jarName = jarName.substring(1);
					if (jarName.length() == 0)
						return null;

					return null;
				}
			}
			catch (MalformedURLException e) {
				throw e;
			}
			catch (IOException e) {
				LOG.ignore(e);
			}
		}
		return resource;
	}

	@Override
	public Set<String> getResourcePaths(String path) {
		// Try to get regular resource paths - this will get appropriate paths from any overlaid wars etc
		Set<String> paths = super.getResourcePaths(path);

		if (path != null) {
			TreeSet<String> allPaths = new TreeSet<String>();
			allPaths.addAll(paths);

			if (path.startsWith(WEB_INF_CLASSES_PREFIX)) {
				int i = 0;

				while (i < webInfClasses.size()) {
					String newPath = path.replace(WEB_INF_CLASSES_PREFIX, webInfClasses.get(i).path);
					allPaths.addAll(super.getResourcePaths(newPath));
					i++;
				}
			}
			return allPaths;
		}
		return paths;
	}


	String addPattern(String s, String pattern) {
		s = s == null ? s = '' : s.trim()

		if (!s.contains(pattern)) {
			if (s.length() != 0)
				s = s + '|'
			s = s + pattern
		}

		s
	}
}
