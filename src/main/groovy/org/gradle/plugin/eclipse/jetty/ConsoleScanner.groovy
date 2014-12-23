package org.gradle.plugin.eclipse.jetty

import groovy.util.logging.Slf4j

/**
 * User: dsv
 * Date: 25.08.13
 * Time: 19:50
 */
@Slf4j
class ConsoleScanner extends Thread {

	final Closure restart;

	public ConsoleScanner(Closure task) {
		this.restart = task;
		name = "Console scanner";
		daemon = true;
	}

	public void run() {
		try {
			while (true) {
				checkSystemInput();
				getSomeSleep();
			}
		} catch (IOException e) {
			log.warn("Error when checking console input.", e);
		}
	}

	private void getSomeSleep() {
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			log.debug("Error while sleeping.", e);
		}
	}

	private void checkSystemInput() throws IOException {
		while (System.in.available() > 0) {
			int inputByte = System.in.read();
			if (inputByte >= 0) {
				char c = (char) inputByte;
				if (c == '\n') {
					restartWebApp();
				}
			}
		}
	}

	/**
	 * Skip buffered bytes of system console.
	 */
	private void clearInputBuffer() {
		try {
			while (System.in.available() > 0) {
				// System.in.skip doesn't work properly. I don't know why
				long available = System.in.available();
				for (int i = 0; i < available; i++) {
					if (System.in.read() == -1) {
						break;
					}
				}
			}
		} catch (IOException e) {
			log.warn("Error discarding console input buffer", e);
		}
	}

	private void restartWebApp() {
		try {
			restart(false);
			// Clear input buffer to discard anything entered on the console
			// while the application was being restarted.
			clearInputBuffer();
		} catch (Exception e) {
			log.error("Error reconfiguring/restarting webapp after a new line on the console", e);
		}
	}
}
