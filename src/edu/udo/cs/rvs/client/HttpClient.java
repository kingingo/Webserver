package edu.udo.cs.rvs.client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import edu.udo.cs.rvs.Header;
import edu.udo.cs.rvs.HttpServer;
import edu.udo.cs.rvs.date.DateFormatException;

/**
 * Client Objekt um die Konversation mit den Client & Server durchzuführen
 * 
 * @author Felix Obenaus (Matrikelnr. 205637)
 */
public class HttpClient extends Thread {
	/**
	 * running = true -> Thread läuft running = false -> Thread ist aus
	 */
	private boolean running = false;
	// InputStream für Request-Header Anfragen
	private BufferedReader in;
	// OutputStream um mit den Client zu kommunizieren
	private DataOutputStream out;

	// Der Client Socket
	private Socket socket;
	// HttpServer der diese Client verbindung angenommen hat.
	private HttpServer server;

	public HttpClient(HttpServer server, Socket socket) {
		this.socket = socket;
		this.server = server;
		start();
	}

	/**
	 * Thread läuft => TRUE Thread ist aus => FALSE
	 * 
	 * @return
	 */
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Startet die Kummunikation zwischen Client und Server
	 */
	public void start() {
		// Überprüft ob der Thread bereits läuft
		if (!this.running) {
			// Zum merken das der Thread nun läuft
			this.running = true;

			// Erstellt alle Streams
			prepareStream();

			// Startet den Thread
			super.start();
		} else
			throw new NullPointerException("Thread is already running");
	}

	/**
	 * Schließt alle ein-/ausgehende Streams, Socket und entfernt den Client von der
	 * Liste (HttpServer.clients)
	 */
	public void close() {
		try {
			// Stoppt den Thread & schließt alle In/Output Streams und den Socket
			this.running = false;
			if (this.in != null)
				this.in.close();
			if (this.out != null)
				this.out.close();
			if (this.socket != null)
				this.socket.close();

			// Entfernt den Client von der Liste
			this.server.remove(this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Erstellt alle benötigten ein-/ausgehenden Streams
	 */
	private void prepareStream() {
		// Falls kein Socket gesetzt wurde, wird eine Exception ausgegeben.
		if (this.socket != null) {
			try {
				// Eingehende Verbindung vom Client
				this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
				// Ausgehende Verbindung zum Client
				this.out = new DataOutputStream(this.socket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else
			throw new NullPointerException("The socket is null!?");
	}

	/**
	 * Zum Auslesen der Anfragen
	 * 
	 * @param request
	 */
	private void readRequest(Header request) {
		System.out.println("REQUEST >>> " + request);

		// Gibt die Http Version des Request Header
		Float version = request.getVersion();

		// Falls die Version über 1.1 ist kann der Webserver nicht damit umgehen.
		if (version.compareTo(1.1F) < 0) {
			writeResponse(StatusCode.BAD_REQUEST);
			return;
		}

		// Spaltet den Kopf der Request (z.b "GET / HTTP/1.1" zu tokens[0]=GET,
		// tokens[1]=/, tokens[2]=HTTP/1.1)
		String[] tokens = request.getHead().split("\\s");

		// Gibt die Request Art aus
		String method = tokens[0];
		switch (method) {
		case "GET":
		case "POST":
			// Gibt den Pfad der aufzurufenden Datei
			String path = tokens[1];
			// Falls über der GET Methode (key,value) angehängt sind (z.b
			// index.html?name=value&name1=value1
			if (path.contains("?")) {
				
				break;
			}
			
			File file = null;
			// Falls keine Datei angegeben ist sondern nur ein Pfad soll eine Index Datei
			// gesucht werden
			if (Utils.isDirectory(path)) {
				// Sucht in dem Pfad ("path") eine Index Datei
				file = Utils.getIndexFile(path);

				// Keine Index Datei wurde gefunden
				if (file == null) {
					// Es wurde keine Index-Datei gefunden also wird der StatusCode No_Content (204)
					// gesendet.
					writeResponse(StatusCode.NO_CONTENT);
					break;
				}
			}else {
				// Sucht die angegebene Datei von "path"
				file = Utils.getFile(path);
			}

			// Falls keine Datei gefunden wurde, wird der 404 File Not Found StatusCode
			// gesendet
			if (file == null) {
				writeResponse(StatusCode.NOT_FOUND, "Die Seite konnte nicht gefunden werden.");
				break;
			} else {
				try {
					if(!file.getCanonicalPath().startsWith(HttpServer.wwwroot.getCanonicalPath())) {
						writeResponse(StatusCode.FORBIDDEN, "This Area of the Website is Forbidden!");
						break;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			// Überprüft ob im Request Header die Opton If-Modified-Since angegeben
			Date if_modified;
			try {
				// Wandelt den String in ein Date Objekt um
				if ((if_modified = Utils.toDate(request.get("If-Modified-Since"))) != null) {
					// Holt sich den Zeitpunkt wann die Datei das letzte mal bearbeitet wurde
					// Zur Zeitzone des Host Computers
					Calendar cal = Calendar.getInstance(TimeZone.getDefault());
					cal.setTimeInMillis(file.lastModified());
					Date last_modified = cal.getTime();
					/*
					 * Falls der letzte Änderungszeitpunkt gleich oder kleiner als der Zeitpunkt von
					 * "if-Modified-Since" ist wird ein der StatusCode not Modified gesendet damit
					 * der Browser die Webseite aus dem Cach lädt
					 **/
					if (if_modified.compareTo(last_modified) >= 0) {
						writeResponse(StatusCode.NOT_MODIFIED);
						break;
					}
				}
			} catch (DateFormatException ex) {
				System.out.println(ex.getMessage());
			}

			// Die Datei wurde gefunden und nun wird auf die Anfrage geantwortet
			writeResponse(file);
			break;
		case "HEAD":
		case "LINK":
		case "UNLINK":
		case "DELETE":
		case "PUT":
			// Sendet zum Browser das diese Anfragen nicht implementiert sind.
			writeResponse(StatusCode.NOT_IMPLEMENTED);
			break;
		default:
			// Er konnte die Anfrage nicht zuordnen also wird ein Server Error
			// zurückgesendet
			sendInternalServerError();
			break;
		}
	}

	/**
	 * Sendet zum Client, das ein Server Error aufgetretten ist.
	 */
	public void sendInternalServerError() {
		writeResponse(StatusCode.INTERNAL_SERVER_ERROR,
				"Die Anfrage konnte nicht bearbeitet werden. Ein Server Fehler ist aufgetretten.");
	}

	/**
	 * Sendet nur einen Header mit einen beliebigen StatusCode zurück zum Client
	 * 
	 * @param code
	 */
	public void writeResponse(StatusCode code) {
		Header.create().addHeadline(code).write(out);
	}

	/**
	 * Zum senden eines Strings zum Client mit einen beliebigen StatusCode
	 * 
	 * @param code
	 * @param content
	 */
	public void writeResponse(StatusCode code, String content) {
		content += "\n";
		Header.create().addHeadline(code).add("Content-length", String.valueOf(content.length())).write(out);

		try {
			out.writeBytes(content);
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Zum senden einer Datei zum Client mit den Status Code OK (200)
	 * 
	 * @param file
	 */
	public void writeResponse(File file) {
		try {
			// Liest die Datei aus und konvertiert alles zu einem byte array
			byte[] file_buffer = Utils.readFile(file);

			/*
			 * Header.create() erstellt einen neuen Header header.addHeadLine setzt den
			 * StatusCode für diesen Header Utils.getContentType(Header, File) setzt den
			 * Content-Type, Content-Length, Last-Modified und ggf. Content-Disposition
			 */
			Header header = Utils.getContentType(Header.create().addHeadline(StatusCode.OK), file);
			// Überprüft ob die Datei im Format UTF-8 sein soll
			if (header.get("Content-Type").contains("charset=utf-8")) {
				// Konvertiert die bytes zu UTF-8
				file_buffer = Utils.toUTF8(file_buffer);

				/*
				 * Falls True dann ist beim Konvertieren ein Fehler aufgetretten somit wird zum
				 * Client übermittelt das etwas schief gelaufen ist mit den StatusCode Internal
				 * Server Error (500)
				 */
				if (file_buffer == null) {
					sendInternalServerError();
					return;
				}
			}

			// Setzt die File länge in den Header
			header.add("Content-length", String.valueOf(file_buffer.length));

			// header.write(OutputStream) sendet den fertigen Header zum Client
			header.write(this.out);

			// Schickt das byte-Array in den OutputStream
			this.out.write(file_buffer);

			// Schickt alles zum Client los
			this.out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Wird ausgeführt falls der Thread gestartet wird und liest die Anfrage aus.
	 */
	public void run() {
		try {
			System.out.println("Open Stream");
			System.out.println();
			// Liest den Request Header aus
			Header request = Header.read(this.in);
			// Liest den Request-Header aus falls er nicht leer war
			if (!request.isEmpty()) {
				readRequest(request);
			} else {
				System.out.println("Request is empty...");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// Schließt alle Verbindungen zum Client.
			close();
			System.out.println("Close Stream");
			System.out.println();
		}
	}
}
