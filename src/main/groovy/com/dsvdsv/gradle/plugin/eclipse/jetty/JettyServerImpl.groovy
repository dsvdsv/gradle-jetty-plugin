package com.dsvdsv.gradle.plugin.eclipse.jetty

import org.eclipse.jetty.server.ShutdownMonitor

//import org.eclipse.jetty.server.*
//import org.eclipse.jetty.server.handler.ContextHandlerCollection
//import org.eclipse.jetty.server.handler.DefaultHandler
//import org.eclipse.jetty.server.handler.HandlerCollection
//import org.eclipse.jetty.server.handler.RequestLogHandler

/**
 * User: Dikansky
 * Date: 21.10.13
 */
class JettyServerImpl implements JettyServer {
	final int DEFAULT_MAX_IDLE_TIME = 30000;
	def server;
	def contexts; //the list of ContextHandlers
	def handlers; //the list of lists of Handlers
	def requestLogHandler; //the request log handler
	def defaultHandler; //default handler

	def requestLog; //the particular request log implementation

	public JettyServerImpl(def server) {
		this.server = server;
	}

	@Override
	public void start() {
		server.start();
	}

	@Override
	public void stop() {
		server.stop();
	}

	@Override
	void join() {
		server.join();
	}

	@Override
	void configureHandlers() {
		def defaultHandler = JettyFactory.instance.loadClass('org.eclipse.jetty.server.handler.DefaultHandler').newInstance();
		def requestLogHandler = JettyFactory.instance.loadClass('org.eclipse.jetty.server.handler.RequestLogHandler').newInstance();
		if (this.requestLog != null)
			requestLogHandler.setRequestLog(this.requestLog);

		contexts = server.getChildHandlerByClass(JettyFactory.instance.loadClass('org.eclipse.jetty.server.handler.ContextHandlerCollection'));
		if (contexts == null) {
			contexts = JettyFactory.instance.loadClass('org.eclipse.jetty.server.handler.ContextHandlerCollection').newInstance();
			def handlerClass = JettyFactory.instance.loadClass('org.eclipse.jetty.server.handler.HandlerCollection');
			def handlers = server.getChildHandlerByClass(handlerClass);
			if (handlers == null) {
				handlers = handlerClass.newInstance()

				handlers.addHandler contexts;
				handlers.addHandler defaultHandler;
				handlers.addHandler requestLogHandler;
			} else {
				handlers.addHandler(contexts);
			}
			server.setHandler(handlers);
		}
	}

	@Override
	void addWebApplication(def webAppContext) {
		contexts.addHandler(webAppContext);
	}

	private def createConnector(int httpPort) {
		def connector = JettyFactory.instance.loadClass('org.eclipse.jetty.server.ServerConnector').newInstance(server);
		connector.setPort(httpPort);
		return connector;
	}

	@Override
	void enableShutdownMonitor(int stopPort, String stopKey, boolean isDaemon) {
		ShutdownMonitor monitor = ShutdownMonitor.getInstance();
		monitor.setPort(stopPort);
		monitor.setKey(stopKey);
		monitor.setExitVm(isDaemon);
		server.setStopAtShutdown(true);
	}

	@Override
	void configureConnector(int httpPort) {
		def connector = createConnector(httpPort);
		server.addConnector connector;
	}
}
