package main;

/**
 * Dies ist die Start-Klasse. Aenderungen an dieser Klassen sind NICHT
 * gestattet!
 *
 * @author Felix Homa
 *
 */
public class Main {
	private static final int DEFAULT_PORT = 80;

	/**
	 * @param arguments the command line arguments
	 */
	public static void main(String[] arguments) {
		int port;
		if (arguments.length == 0) {
			port = DEFAULT_PORT;
			System.out.println("No Port Argument given. Defaulting to "+DEFAULT_PORT);
		} else if (arguments.length == 1) {
			if (arguments[0].length() > 0 && 
					(arguments[0].substring(1).toLowerCase().startsWith("h") || 
					 arguments[0].toLowerCase().startsWith("-h") || 
					 arguments[0].toLowerCase().startsWith("?"))) {
				System.out.println("java -jar <jar-File> [port]");
				System.out.println("<jar-File> \tThis Jar File");
				System.out.println("[port]     \tThe Port to run on. Default: " + DEFAULT_PORT);
				System.exit(1);
				return;
			}
			try {
				port = Integer.parseInt(arguments[0]);
			} catch (NumberFormatException nfexc) {
				System.err.println("First argument not a number! Exiting! " + arguments[0]);
				System.exit(1);
				return;
			}
		} else {
			System.err.println("Too Many Arguments!");
			System.exit(1);
			return;
		}

		HttpServer server = new HttpServer(port);
		server.startServer();
	}
}
