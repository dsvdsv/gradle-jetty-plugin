package org.gradle.plugin.eclipse.jetty

import groovy.util.logging.Slf4j
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.webapp.WebAppClassLoader
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.webapp.WebInfConfiguration

/**
 * User: Dikansky
 * Date: 28.10.13
 */
@Slf4j
class GradleWebInfConfiguration extends WebInfConfiguration {

	@Override
	public void configure(WebAppContext context) throws Exception {
		JettyWebAppContext jwac = context as JettyWebAppContext;

		if (jwac.classPathFiles != null) {
			log.debug 'Setting up classpath ...';
			WebAppClassLoader classLoader = context.getClassLoader() as WebAppClassLoader;
			jwac.classPathFiles.each { File f -> classLoader.addClassPath(f.canonicalPath) };
		}

		super.configure(context);
	}

	@Override
	protected List<Resource> findJars(WebAppContext context)
	throws Exception {
		List<Resource> list = new ArrayList<Resource>();
		JettyWebAppContext jwac = context as JettyWebAppContext;
		if (jwac.getClassPathFiles() != null) {
			jwac.getClassPathFiles().each { File f ->
				if (f.getName().toLowerCase(Locale.ENGLISH).endsWith('.jar')) {
					try {
						list.add(Resource.newResource(f.toURI()));
					}
					catch (Exception e) {
						log.warn('Bad url ', e);
					}
				}
			}
		}

		List<Resource> superList = super.findJars(context);
		if (superList != null)
			list.addAll(superList);
		return list;
	}
}
