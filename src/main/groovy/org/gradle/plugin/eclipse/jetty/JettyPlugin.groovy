package org.gradle.plugin.eclipse.jetty

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.plugins.WarPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.War

public class JettyPlugin implements Plugin<Project> {
	private static final String CONFIGURATION_NAME = 'eclipseJetty';
	public static final String JETTY_RUN = 'jettyRun';
	public static final String JETTY_RUN_WAR = 'jettyRunWar';
	public static final String JETTY_STOP = 'jettyStop';
	public static final String FARM_JETTY_RUN = 'farmJettyRun';
	public static final String FARM_JETTY_STOP = 'farmJettyStop'

	@Override
	void apply(Project project) {
		project.configurations.create(CONFIGURATION_NAME).setVisible(false).setTransitive(true)
				.setDescription('The Jetty libraries to be used for this project.');

		if(project.plugins.findPlugin(org.gradle.api.plugins.WarPlugin)) {
			JettyPluginConvention convention = new JettyPluginConvention();

			project.convention.plugins.eclipseJetty = convention;
			configureMappingRules(project, convention);
			configureJettyStop(project, convention);
			configureJettyRun(project, convention);
			configureJettyRunWar(project, convention);
		} else {
			FarmJettyPluginConvention convention = new FarmJettyPluginConvention();
			project.convention.plugins.farmJetty = convention;
			configureFarmJettyRun(project, convention);
			configureFarmJettyStop(project, convention);
		}
	}

	void configureFarmJettyRun(Project project, final FarmJettyPluginConvention convention) {
		project.tasks.withType(FarmJettyRunTask).whenTaskAdded { FarmJettyRunTask task ->
			task.dependsOn(JavaPlugin.CLASSES_TASK_NAME)
			task.logging.level = LogLevel.INFO;
			task.daemon = false
			task.reloadable = true
			task.conventionMapping.map('stopPort') { convention.stopPort }
			task.conventionMapping.map('stopKey') { convention.stopKey }
			task.conventionMapping.map('httpPort') { convention.httpPort }
			task.conventionMapping.map('buildscriptClasspath') { project.buildscript.configurations.getByName('classpath').asFileTree }
			task.conventionMapping.map('jettyClasspath') { project.configurations.getByName(CONFIGURATION_NAME).asFileTree }
		}
		FarmJettyRunTask farmJettyRun = project.tasks.create(FARM_JETTY_RUN, FarmJettyRunTask);
		farmJettyRun.description = 'Run Jetty.'

	}

	void configureFarmJettyStop(Project project, final FarmJettyPluginConvention convention) {
		FarmJettyStopTask farmJettyStop = project.tasks.create(FARM_JETTY_STOP, FarmJettyStopTask);
		farmJettyStop.description = 'Stops Jetty.';

		farmJettyStop.conventionMapping.map('stopPort') { convention.stopPort }
		farmJettyStop.conventionMapping.map('stopKey') { convention.stopKey }
	}

	void configureJettyRunWar(Project project, JettyPluginConvention jettyPluginConvention) {
		project.tasks.withType(JettyRunWarTask).whenTaskAdded { JettyRunWarTask jettyRunWar ->
			jettyRunWar.dependsOn(WarPlugin.WAR_TASK_NAME)
			jettyRunWar.conventionMapping.map('webApp') { project.tasks.getByName(WarPlugin.WAR_TASK_NAME).archivePath }
		}

		JettyRunWarTask jettyRunWar = project.tasks.create(JETTY_RUN_WAR, JettyRunWarTask)
		jettyRunWar.description = 'Assembles the webapp into a war and deploys it to Tomcat.'
		jettyRunWar.group = WarPlugin.WEB_APP_GROUP
	}

	void configureMappingRules(final Project project, final JettyPluginConvention convention) {
		project.tasks.withType(AbstractJettyTask).whenTaskAdded { AbstractJettyTask jettyTask ->
			jettyTask.logging.level = LogLevel.INFO;
			jettyTask.daemon = false
			jettyTask.reloadable = true
			jettyTask.conventionMapping.map('buildscriptClasspath') { project.buildscript.configurations.getByName('classpath').asFileTree }
			jettyTask.conventionMapping.map('jettyClasspath') { project.configurations.getByName(CONFIGURATION_NAME).asFileTree }
			jettyTask.conventionMapping.map('stopPort') { convention.stopPort }
			jettyTask.conventionMapping.map('stopKey') { convention.stopKey }
			jettyTask.conventionMapping.map('httpPort') { convention.httpPort }
			jettyTask.conventionMapping.map('jettyEnvXml') { convention.jettyEnvXml }
			jettyTask.conventionMapping.map('extraResourceBases') { convention.extraResourceBases }
			jettyTask.conventionMapping.map('contextPath') { project.tasks.getByName(WarPlugin.WAR_TASK_NAME).baseName }
		}
	}

	def configureJettyRun(Project project, JettyPluginConvention convention) {
		project.tasks.withType(JettyRunTask).whenTaskAdded { JettyRunTask task ->
			task.dependsOn(JavaPlugin.CLASSES_TASK_NAME)
		}
		JettyRunTask jettyRun = project.tasks.create(JETTY_RUN, JettyRunTask);
		jettyRun.description = 'Run Jetty.'
		jettyRun.group = WarPlugin.WEB_APP_GROUP;

		jettyRun.conventionMapping.map('webAppSourceDirectory') { getWarConvention(project).webAppDir }
		jettyRun.conventionMapping.map('webAppClasses') { getJavaConvention(project).sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).runtimeClasspath }
		jettyRun.conventionMapping.map('webXml') { getWebXml(project) }
	}

	def getWebXml(Project project) {
		War war = project.tasks.getByName(WarPlugin.WAR_TASK_NAME);
		File webXml;
		if (war.webXml) {
			webXml = war.webXml
		} else {
			webXml = new File(getWarConvention(project).webAppDir, 'WEB-INF/web.xml');
		}
		webXml;
	}

	def configureJettyStop(Project project, JettyPluginConvention convention) {
		JettyStopTask jettyStop = project.tasks.create(JETTY_STOP, JettyStopTask);
		jettyStop.description = 'Stops Jetty.';
		jettyStop.group = WarPlugin.WEB_APP_GROUP;

		jettyStop.conventionMapping.map('stopPort') { convention.stopPort }
		jettyStop.conventionMapping.map('stopKey') { convention.stopKey }
	}

	WarPluginConvention getWarConvention(Project project) {
		project.convention.getPlugin(WarPluginConvention)
	}

	JavaPluginConvention getJavaConvention(Project project) {
		project.convention.getPlugin(JavaPluginConvention)
	}


}