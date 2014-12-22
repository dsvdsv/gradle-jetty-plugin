package org.gradle.plugin.eclipse.jetty

import org.gradle.api.Project
import org.gradle.api.plugins.WarPlugin

/**
 * User: Dikansky
 * Date: 22.12.2014
 */
class Utils {
	private Utils() {}

	static Collection<Project> projects(Project prj) {
		List<Project> result = new ArrayList<>();
		prj.childProjects.each { String name, Project p ->
			result.addAll(projects(p))
			result.add(p)
		}
		return result;
	}

	static boolean isWebApp(Project project) {
		project.plugins.hasPlugin(WarPlugin)
	}
}
