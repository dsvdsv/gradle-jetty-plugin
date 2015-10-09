package com.dsvdsv.gradle.plugin.eclipse.jetty

import groovy.util.logging.Slf4j
import org.eclipse.jetty.annotations.AnnotationConfiguration
import org.eclipse.jetty.annotations.AnnotationParser
import org.eclipse.jetty.annotations.ClassNameResolver
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.webapp.MetaData
import org.eclipse.jetty.webapp.WebAppContext

/**
 * User: dsv
 * Date: 25.08.13
 * Time: 19:20
 */
@Slf4j
class GradleAnnotationConfiguration extends AnnotationConfiguration {

	@Override
	public void parseWebInfClasses(final WebAppContext context, final AnnotationParser parser) throws Exception {
		JettyWebAppContext jwac = context as JettyWebAppContext;
		if (jwac.classPathFiles == null || jwac.classPathFiles.size() == 0)
			super.parseWebInfClasses(context, parser);
		else {
			log.debug('Scanning classes ');
			//Look for directories on the classpath and process each one of those

			MetaData metaData = context.metaData;
			if (metaData == null)
				throw new IllegalStateException('No metadata');

//			_discoverableAnnotationHandlers.each {
//				if (it instanceof AbstractDiscoverableAnnotationHandler) {
//					it.setResource(null);
//				}
//			};
			Set<AnnotationParser.Handler> handlers = new HashSet<AnnotationParser.Handler>();
			handlers.addAll(_discoverableAnnotationHandlers);
			handlers.add(_classInheritanceHandler);
			handlers.addAll(_containerInitializerAnnotationHandlers);

			for (it in jwac.classPathFiles) {
				//scan the equivalent of the WEB-INF/classes directory that has been synthesised by the plugin
				if (it.isDirectory() && it.exists()) {
					doParse(handlers, context, parser, Resource.newResource(it.toURI()));
				}
			};

			//if an actual WEB-INF/classes directory also exists (eg because of overlayed wars) then scan that
			//too
			if (context.webInf != null && context.webInf.exists()) {
				Resource classesDir = context.webInf.addPath('classes/');
				if (classesDir.exists()) {
					doParse(handlers, context, parser, classesDir);
				}
			}
		}
	}

	def doParse(final def handlers, final def context, final AnnotationParser parser, def resource) {
		parser.parseDir(handlers, resource, [
				isExcluded    : { String name ->
					if (context.isSystemClass(name)) return true;
					if (context.isServerClass(name)) return false;
					return false;
				},
				shouldOverride: { String name ->
					//looking at webapp classpath, found already-parsed class of same name - did it come from system or duplicate in webapp?
					if (context.isParentLoaderPriority())
						return false;
					return true;
				}
		] as ClassNameResolver);
	}
}
