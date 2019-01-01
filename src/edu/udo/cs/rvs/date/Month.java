package edu.udo.cs.rvs.date;

/**
 * Enum für HttpDateFormat womit die Monate erkannt werden können.
 * 
 * @author Felix Obenaus (Matrikelnr 205637)
 */
public enum Month {
JANUARY("Jan"),
FEBRUARY("Feb"),
MARCH("Mar"),
APRIL("Apr"),
MAY("May"),
JUNE("Jun"),
JULY("Jul"),
AUGUST("Aug"),
SEPTEMBER("Sep"),
OCTOBER("Oct"),
NOVEMBER("Nov"),
DECEMBER("Dec");


private String monthName;

private Month(String monthName) {
	this.monthName=monthName;
}

public String getMonthName() {
	return this.monthName;
}

}
