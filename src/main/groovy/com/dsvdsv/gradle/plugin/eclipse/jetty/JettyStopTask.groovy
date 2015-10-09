package com.dsvdsv.gradle.plugin.eclipse.jetty

import groovy.util.logging.Slf4j
import org.gradle.api.InvalidUserDataException
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.TaskAction
import org.gradle.logging.ProgressLogger
import org.gradle.logging.ProgressLoggerFactory

/**
 * User: dsv
 * Date: 18.08.13
 * Time: 22:02
 */
@Slf4j
class JettyStopTask extends ConventionTask {

	Integer stopPort;

	Integer stopWait;

	String stopKey;

	@TaskAction
	void stop() {
		if (getStopPort() == null) {
			throw new InvalidUserDataException('Please specify a valid port');
		}
		if (getStopKey() == null) {
			throw new InvalidUserDataException('Please specify a valid stopKey');
		}

		ProgressLogger progressLogger = getServices().get(ProgressLoggerFactory).newOperation(JettyStopTask);
		progressLogger.setDescription('Stop Jetty server');
		progressLogger.setShortDescription('Stopping Jetty');
		progressLogger.started();

		try {
			Socket s = new Socket(InetAddress.getByName('127.0.0.1'), getStopPort());
			s.setSoLinger(false, 0);

			OutputStream out = s.getOutputStream();
			out.write((getStopKey() + '\r\nstop\r\n').getBytes());
			out.flush();

			if (getStopWait() != null) {
				s.setSoTimeout(getStopWait() * 1000);
				s.getInputStream();

				log.info("Waiting '$getStopWait()' seconds for jetty to stop");
				LineNumberReader lin = new LineNumberReader(new InputStreamReader(s.getInputStream()));
				String response;
				boolean stopped = false;
				while (!stopped && ((response = lin.readLine()) != null)) {
					if ('Stopped'.equals(response)) {
						stopped = true;
						log.info('Server reports itself as Stopped');
					}
				}
			}
			s.close();
		} catch (ConnectException e) {
			log.info('Jetty not running!');
		} catch (Exception e) {
			log.error('Exception during stopping', e);
		} finally {
			progressLogger.completed();
		}
	}
}