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
 * @author Felix Obenaus (Matrikelnr 205637)
 */
public class HttpDateFormat {
	private String format;
	private Calendar calendar;
	private TimeZone zone;

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
	public TimeZone getTimeZone(String dateString) {
		for(String id : TimeZone.getAvailableIDs()) {
			if(dateString.contains(id)) {
				return TimeZone.getTimeZone(id);
			}
		}
		return null;
	}

	/**
	 * Konvertiert String zu einem Datum in der Zeitzone this.zone
	 * Zum starten der parse(int,String) Methode und um die Zeitzone anzupassen
	 */
	public Date parse(String dateString) throws DateFormatException {
		//Um zu ermitteln ob eine Zeitzone im String angegeben wurde.
		TimeZone tz = getTimeZone(dateString);
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
			Date date = parse(0, dateString.replaceAll(tz.getID(), ""));
			long offset_utc = -tz.getRawOffset(); //zu UTC
			long offset_zone = this.zone.getRawOffset(); //zu this.zone
			
			date.setTime( date.getTime() + offset_utc + offset_zone);
			return date;
		}
		
		return parse(0, dateString);
	}

	/**
	 * Wandelt einen String zu einen Date Objekt
	 * eine Rekursive Funktion endet bis Index > format.length
	 */
	private Date parse(int index, String dateString) throws DateFormatException {
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
			return parse(index + 1, dateString);

		switch (c) {
		case 'y'://yyyy z.b 2019
			index = checkInteger(Calendar.YEAR, dateString, c, index, 4);
			break;
		case 'M'://MMM z.b Jan
			if (charInARow(index, index + 3, c)) {
				//Ermittelt den angegeben Moant aus dem String
				Month month = getMonth(dateString, index, index + 3);
				//Setzt den Monat fest der festgestellt wurde.
				this.calendar.set(Calendar.MONTH, month.ordinal());
				index += (3 + 1);
			}
			break;
		case 'd'://dd z.b 01-31
			index = checkInteger(Calendar.DAY_OF_MONTH, dateString, c, index, 2);
			break;
		case 'E':
			if (charInARow(index, index + 3, c)) {
				/*
				 *  Wird übersprungen weil der Tag (Mon,Tue,Wed,Thu,Fri,Sat,Sun) ist nicht wichtig
				 *  eher der Tag im Monat dd (0-31)
				 */
				index += (3 + 1);
			} else {
				throwException();
			}
			break;
		case 'H'://HH z.b 0-24
			index = checkInteger(Calendar.HOUR_OF_DAY, dateString, c, index, 2);
			break;
		case 'm'://mm z.b 0-60
			index = checkInteger(Calendar.MINUTE, dateString, c, index, 2);
			break;
		case 's'://ss z.b 0-60
			index = checkInteger(Calendar.SECOND, dateString, c, index, 2);
			break;
		default:
			//Kein Pattern passt zu den Char... Exception
			throw new IllegalArgumentException("Couldn't match this char " + c);
		}

		return parse(index, dateString);
	}

	/**
	 * Liest aus dem @dateString eine Zahl aus die aus dem char @c besteht
	 * und von @index bis @index+@add geht und setzt es im Calendar unter den @field fest.
	 * der neue @index wird zurückgegeben
	 */
	private int checkInteger(int field, String dateString, char c, int index, int add) throws DateFormatException {
		//Überprüft ob der char @c im String @format vom Index @index bis @index+@add geht.
		if (charInARow(index, index + add, c)) {
			//Wandelt die Zahl am Index @index bis @index+@add aus dem String dateString in eine Zahl um
			int value = getInteger(dateString, index, index + add);
			//Fügt es zum Kalendar hinzu
			this.calendar.set(field, value);
		} else {
			throwException();
		}
		return index + (add + 1);
	}

	/**
	 * Gibt den Monat aus dem String @dateString aus
	 */
	private Month getMonth(String dateString, int from, int to) throws DateFormatException {
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
	private Integer getInteger(String dateString, int from, int to) throws DateFormatException {
		try {
			String subString = dateString.substring(from, to);
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