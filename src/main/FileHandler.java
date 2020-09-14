package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

public class FileHandler {
	private static FileHandler instance;
	
	public static FileHandler getInstance() {
		if(instance==null)instance=new FileHandler();
		return instance;
	}
	
	private HashMap<String, Semaphore> semaphores = new HashMap<>();
	
	private FileHandler() {}
	
	/**
	 * Gibt einen Semaphore zur�ck der zu dieser File zu geordnet wurde.
	 */
	private Semaphore getSemaphore(File file) throws IOException {
		Semaphore sem = null;
		String path = file.getCanonicalPath();
		
		if( (sem = this.semaphores.get(path)) == null ) {
			sem = new Semaphore(1);
			this.semaphores.put(path, sem);
		}
		return sem;
	}
	
	/**
	 * Liest aus einer Datei die bytes aus und speichert sie in einen byte-Array und
	 * gibt dieses zurueck
	 */
	public byte[] readFile(File file) throws IOException {
		Semaphore sem = getSemaphore(file);
		// InputStream extra fuer Datei um diese einfach Auszulesen
		FileInputStream file_input = null;
		// Erstellt ein byte-Array welches gross genug fuer die Datei ist.
		byte[] buffer = new byte[(int) file.length()];
		try {
			//Stellt sicher das immer nur ein Thread read ausf�hrt f�r die gleiche Datei
			sem.acquire();
			// Erstellt ein FileInputStream mit der angegeben Datei (file)
			file_input = new FileInputStream(file);
			// Liest den InputStream aus und schreibt alles in das byte-Array (buffer)
			file_input.read(buffer);
		} 
		catch (InterruptedException e) {
			e.printStackTrace();
		} 
		finally {
			// Schliesst den FileInputStream
			file_input.close();
			//Gibt den Semaphore wieder frei
			sem.release();
		}
		return buffer;
	}
}
