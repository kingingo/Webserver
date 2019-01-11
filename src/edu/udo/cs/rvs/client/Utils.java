package edu.udo.cs.rvs.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import edu.udo.cs.rvs.FileHandler;
import edu.udo.cs.rvs.Header;
import edu.udo.cs.rvs.HttpServer;
import edu.udo.cs.rvs.date.DateFormatException;
import edu.udo.cs.rvs.date.HttpDateFormat;

/**
 * Nützliche einzelne Methoden
 * 
 * @author Felix Obenaus (Matrikelnr. 205637)
 * @author Abdulhamed Chribati (Matrikelnr. 206317)
 */
public class Utils {
	private static final long SECOND = 1000;
	private static final long MINUTE = SECOND * 60;
	private static final long HOUR = MINUTE * 60;
	private static final long DAY = HOUR * 24;

	/**
	 * Konvertiert milisekunden zu DD HH:mm:ss
	 * 
	 * Hab ich zum Testen von der HttpDateFormat funktion benutzt.
	 */
	public static String convertMilis(long milis) {
		if (milis > MINUTE) {
			if (milis > HOUR) {
				if (milis > DAY) {
					int time = (int) (milis / DAY);
					if (milis - time * DAY > 1) {
						return time + " days " + convertMilis(milis - time * DAY);
					}
					return time + " day";
				}

				int time = (int) (milis / HOUR);
				if (milis - time * HOUR > 1) {
					return time + "h " + convertMilis(milis - time * HOUR);
				}
				return time + "h";
			}

			int time = (int) (milis / MINUTE);
			if (milis - time * MINUTE > 1) {
				return time + "min " + convertMilis(milis - time * MINUTE);
			}
			return time + "min";
		}

		return (int) (milis / SECOND) + "sec";
	}

	/**
	 * Fügt zum Header den Passenden Content-Type je nach Dateientyp hinzu
	 * 
	 * Darf nur von innerhalb von Utils aufgerufen werden (private)
	 */
	private static Header getContentType(Header header, String filename) {
		String value = null;
		// Überprüft ob der filename nicht leer ist
		if (!filename.isEmpty()) {
			// Setzt den richtigen Content-Type je nach Dateityp
			if (filename.endsWith(".txt"))
				value = "text/plain; charset=utf-8";
			else if (filename.endsWith(".htm") || filename.endsWith(".html"))
				value = "text/html; charset=utf-8";
			else if (filename.endsWith(".css"))
				value = "text/css; charset=utf-8";
			else if (filename.endsWith(".ico"))
				value = "image/x-icon";
			else if (filename.endsWith(".jpg"))
				value = "image/jpeg";
			else if (filename.endsWith(".png"))
				value = "image/png";
			else if (filename.endsWith(".pdf"))
				value = "application/pdf";
			else
				value = "application/octet-stream";
		} else {
			value = "application/octet-stream";
		}

		// Fügt zum Header den Content-Type hinzu
		header.add("Content-Type", value);
		return header;
	}

	/**
	 * Konvertiert @param milis zu einen String im GMT Format
	 */
	@SuppressWarnings("deprecation")
	public static String toDateString(long milis) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(milis);
		return cal.getTime().toGMTString();
	}
	
	private static HttpDateFormat[] formats = new HttpDateFormat[] {
			new HttpDateFormat("dd MMM yyyy HH:mm:ss", TimeZone.getDefault()),
			new HttpDateFormat("EEE, dd MMM yyyy HH:mm:ss", TimeZone.getDefault())
	};

	/**
	 * Konvertiert @param value (String) zu einen Date-Objekt. z.b "Sat, 29 Oct 1994
	 * 19:43:31 GMT"
	 * 
	 * @throws DateFormatException
	 */
	public static Date toDate(String value) throws DateFormatException {
		if (value == null)
			return null;
		return toDate(0, value);
	}

	/**
	 * Probiert alle Formate aus formats bis ein passendes gefunden wurde oder wirft
	 * eine Exception falls kein pattern passt
	 * 
	 * Falls das Datum in der Zeitzone GMT ist wird es automatisch an die Zeitzone
	 * des Computer angepasst.
	 * 
	 * @throws DateFormatException
	 */
	private static Date toDate(int index, String value) throws DateFormatException {
		if (index >= formats.length)
			throw new DateFormatException("Didn't find a parser which suit to the date " + value);
		try {
			return formats[index].parse(value);
		} catch (Exception e) {
			return toDate(index + 1, value);
		}
	}

	/**
	 * Setzt den Content-Typ, Content-length, Last-Modified ggf. auch
	 * Content-Disposition falls es eine PDF Datei ist.
	 */
	public static Header getContentType(Header header, File file) {
		// Setzt den richtigen Content-Typ für die Datei @file in den @header
		getContentType(header, file.getName());

		/*
		 * Falls es eine PDF Datei ist wird im Header "Content-Disposition" gesetzt
		 * Damit die PDF Datei aufrufbar ist
		 */
		if (file.getName().endsWith(".pdf")) {
			header.add("Content-Disposition", "inline; filename=\"" + file.getName() + "\"");
		}
		// Setzt den letzten Änderungszeitpunkt der Datei in den Header
		header.add("Last-Modified", toDateString(file.lastModified()));
		return header;
	}
	
	/**
	 * Gibt den richtigen Pfad + ROOT Pfad zurück
	 */
	public static File toFile(String path) {
		// Erstellt ein File Objekt mit den Pfad ROOT+PATH
		return new File(HttpServer.wwwroot.getAbsolutePath() + path);
	}
	
	/**
	 * Überprüft ob die @file ein Ordner ist oder nicht.
	 */
	public static boolean isDirectory(String path) {
		File file = toFile(path);
		return file.isDirectory();
	}

	/**
	 * Überprüft ob eine Datei vorhanden im Pfad ROOT_PFAD + path falls ja wird die
	 * Datei zurückgegeben falls nicht wird null zurückgegeben.
	 */
	public static File getFile(String path) {
		File file = toFile(path);
		// Falls die Datei existiert wird sie zurückgegeben
		if (file.exists())
			return file;
		return null;
	}

	/**
	 * Konvertiert ein byte-Array zu einen byte-Array in UTF8 Format!
	 */
	public static byte[] toUTF8(byte[] original) {
		byte[] utf8 = null;
		try {
			utf8 = new String(original).getBytes("UTF8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return utf8;
	}

	/**
	 * Sucht im Pfad (ROOT+PATH) eine Index Datei Falls keine gefunden wird gibt die
	 * Methode null zurück und sonst die Datei
	 */
	public static File getIndexFile(String path) {
		path = HttpServer.wwwroot.getAbsolutePath() + path;

		if (existFile(path + File.separatorChar + "index.php")) {
			return new File(path + File.separatorChar + "index.php");
		}else if (existFile(path + File.separatorChar + "index.html")) {
			return new File(path + File.separatorChar + "index.html");
		} else if (existFile(path + File.separatorChar + "index.htm")) {
			return new File(path + File.separatorChar + "index.htm");
		} else if (existFile(path + File.separatorChar + "index.txt")) {
			return new File(path + File.separatorChar + "index.txt");
		} else
			return null;
	}

	/**
	 * Überprüft ob die Datei im Pfad (path) exestiert FALSE => NEIN sie exestiert
	 * nicht TRUE => JA sie exestiert
	 */
	public static boolean existFile(String path) {
		File file = new File(path);
		return file.exists();
	}

	/**
	 * Liest aus einer Datei die bytes aus und speichert sie in einen byte-Array und
	 * gibt dieses zurueck
	 */
	public static byte[] readFile(File file) throws IOException {
		return FileHandler.getInstance().readFile(file);
	}
}
