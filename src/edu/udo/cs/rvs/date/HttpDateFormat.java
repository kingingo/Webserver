package edu.udo.cs.rvs.date;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Jedes HttpDateFormat Objekt kann nur einen String im gleichen Moment
 * umwandeln deswegen sollte man die clone() funktion verwenden.
 * 
 * Um einen String in dein Date Objekt zu konvertieren
 * Pattern:
 * yyyy: Jahr z.b 2019
 * MMM: Monat z.b Jan, Feb, Mar, Apr... Dec
 * dd: Tag z.b 01..31
 * HH: Stunden z.b 00..24
 * mm: Minuten z.b 00..60
 * ss: Sekunden z.b 00..60
 * 
 * Desweiteren erkennt der Pattern auch TimeZone.IDs z.b GMT oder UTC
 * 
 * @author Felix Obenaus (Matrikelnr. 205637)
 * @author Abdulhamed Chribati (Matrikelnr. 206317)
 */
public class HttpDateFormat {
	private String format;
	private Calendar calendar;
	private TimeZone zone;
	private String dateString;
	/**
	 * @param format -> der Pattern
	 * @param zone -> die Zeitzone für das Datum
	 */
	public HttpDateFormat(String format, TimeZone zone) {
		if (format == null || format.isEmpty() || zone == null) {
			throw new NullPointerException("Some arguments are null or empty");
		}
		this.zone=zone;
		this.format = format;
		//Erstellt das Calendar Objekt welche das eigentliche Datum erstellt.
		initCalendar();
	}

	public HttpDateFormat clone() {
		return new HttpDateFormat(this.format, this.zone);
	}
	
	/**
	 * Erstellt ein Calendar Objekt, womit das Datum erstellt wird.
	 */
	private void initCalendar() {
		this.calendar = Calendar.getInstance(zone);
	}
	
	/**
	 * Ermittelt die Angegebene Zeitzone die im dateString
	 * z.b Sat, 29 Oct 1994 19:43:31 GMT -> @return GMT
	 */
	public TimeZone getTimeZone() {
		for(String id : TimeZone.getAvailableIDs()) {
			if(dateString.contains(id)) {
				dateString=dateString.replaceAll(id, "");
				return TimeZone.getTimeZone(id);
			}
		}
		return null;
	}

	public Date parse(String dateString) throws DateFormatException {
		return clone().parse0(dateString);
	}
	
	/**
	 * Konvertiert String zu einem Datum in der Zeitzone this.zone
	 * Zum starten der parse(int,String) Methode und um die Zeitzone anzupassen
	 */
	private Date parse0(String dateString) throws DateFormatException {
		this.dateString=dateString;
		
		//Um zu ermitteln ob eine Zeitzone im String angegeben wurde.
		TimeZone tz = getTimeZone();
		/*
		 * Falls ja wird der String zu einem Datum umgewandelt und dann wird 
		 * der Offset hinzu subtrahiert um das Datum in die Zeitzone UTC zubringen
		 * und dann wird nochmal der Offset addiert um das Datum von UTC auf this.zone
		 * zu bringen.
		 */
		if(tz!=null) {
			/*
			 * Der String wird zu einem Date umgewandelt.
			 * mit replaceAll wird die angegebene Zeitzone entfernt.
			 */
			Date date = parse(0);
			long offset_utc = -tz.getRawOffset(); //zu UTC
			long offset_zone = this.zone.getRawOffset(); //zu this.zone
			
			date.setTime( date.getTime() + offset_utc + offset_zone);
			return date;
		}
		
		return parse(0);
	}

	/**
	 * Wandelt einen String zu einen Date Objekt
	 * eine Rekursive Funktion endet bis Index > format.length
	 */
	private Date parse(int index) throws DateFormatException {
		//Stopp Bedingung
		if (index > this.format.length()) {
			//Das Datum wird ausgegeben von Calendar
			Date date = this.calendar.getTime();
			//der Calendar wird wieder auf null gesetzt für den nächsten durchlauf
			this.calendar.clear();
			return date;
		}

		/*
		 * Geht jeden Index durch und überprüft ob ein Muster gefunden wird je nach Pattern
		 * yyyy: Jahr z.b 2019
		 * MMM: Monat z.b Jan, Feb, Mar, Apr... Dec
		 * dd: Tag z.b 01..31
		 * HH: Stunden z.b 00..24
		 * mm: Minuten z.b 00..60
		 * ss: Sekunden z.b 00..60
		 * 
		 */
		char c = this.format.charAt(index);
		
		//Leerzeichen, Kommas oder Striche sollen übersprungen werden.
		if (c == ' ' || c == '-' || c == ',')
			return parse(index + 1);

		switch (c) {
		case 'y'://yyyy z.b 2019
			index = checkInteger(Calendar.YEAR, c, index, 4);
			break;
		case 'M'://MMM z.b Jan
			if (charInARow(index, index + 3, c)) {
				//Ermittelt den angegeben Moant aus dem String
				Month month = getMonth(index, index + 3);
				//Setzt den Monat fest der festgestellt wurde.
				this.calendar.set(Calendar.MONTH, month.ordinal());
				index += (3 + 1);
			}
			break;
		case 'd':
			index = checkInteger(Calendar.DAY_OF_MONTH, c, index, 2);
			break;
		case 'E':
			if (charInARow(index, index + 3, c)) {
				/*
				 *  Wird ܼuebersprungen weil der Tag (Mon,Tue,Wed,Thu,Fri,Sat,Sun) ist nicht wichtig
				 *  eher der Tag im Monat dd (00-31)
				 */
				index += (3 + 1);
			} else {
				throwException();
			}
			break;
		case 'H'://HH z.b 0-24
			index = checkInteger(Calendar.HOUR_OF_DAY, c, index, 2);
			break;
		case 'm'://mm z.b 0-60
			index = checkInteger(Calendar.MINUTE, c, index, 2);
			break;
		case 's'://ss z.b 0-60
			index = checkInteger(Calendar.SECOND, c, index, 2);
			break;
		default:
			//Kein Pattern passt zu den Char... Exception
			throw new IllegalArgumentException("Couldn't match this char " + c);
		}

		return parse(index);
	}
	
	/**
	 * Liest aus dem @dateString eine Zahl aus die aus dem char @c besteht
	 * und von @index bis @index+@add geht und setzt es im Calendar unter den @field fest.
	 * der neue @index wird zurueckgegeben
	 */
	private int checkInteger(int field, char c, int index, int add) throws DateFormatException {
		//Ueberprueft ob der char @c im String @format vom Index @index bis @index+@add geht.
		if (charInARow(index, index + add, c)) {
			String subString = substring(index, index + add);
			//Es wird geprueft ob subString nur Zahlen hat 
			String trimed = trim(subString);
			if(subString.length() != trimed.length()) {
				//dateString muss erweitert werden damit der Index weiterhin passt...
				dateString = dateString.substring(0, index)+" "+dateString.substring(index, dateString.length());
				subString = trimed;
			}
			
			//Wandelt die Zahl am Index @index bis @index+@add aus dem String dateString in eine Zahl um
			int value = getInteger(subString);
			//Faegt es zum Kalendar hinzu
			this.calendar.set(field, value);
		} else {
			throwException();
		}
		return index + (add + 1);
	}
	
	private String substring(int from, int to) {
		if(to >= dateString.length())to=dateString.length();
		return dateString.substring(from, to);
	}
	
	/**
	 * Entfernt alles au�er Zahlen ausdem String.
	 */
	private String trim(String value) {
		return value.replaceAll("[^0-9]","");
	}

	/**
	 * Gibt den Monat aus dem String @dateString aus
	 */
	private Month getMonth(int from, int to) throws DateFormatException {
		//Schneidet aus dem String einen subString raus von @from bis @to
		String subString = dateString.substring(from, to);
		
		//Überprüft ob dies ein gültiger Monat ist.
		Month month = null;
		for (Month m : Month.values()) {
			if (m.getMonthName().equalsIgnoreCase(subString)) {
				month = m;
				break;
			}
		}

		//Falls keiner Gefunden wurde passt das Datum zum Pattern nicht.
		if (month == null)
			throwException();
		return month;
	}

	/**
	 * Wandelt einen String zu einem Integer um
	 */
	private Integer getInteger(String subString) throws DateFormatException {
		try {
			return Integer.valueOf(subString);
		} catch (NumberFormatException e) {
		}
		throw new DateFormatException("The Date doesn't suit to the pattern.");
	}

	/**
	 * Wirft eine Exception ;)
	 */
	private void throwException() throws DateFormatException {
		throw new DateFormatException("The Date doesn't suit to the pattern.");
	}

	/**
	 * Überprüft ob sich ein Char @c im String @format vom Index @from bis zum Index @to befinden
	 * Falls ja => TRUE sonst FALSE
	 */
	private boolean charInARow(int from, int to, char c) {
		for (int index = from; index < to; index++) {
			if (format.charAt(index) != c)
				return false;
		}
		return true;
	}
}