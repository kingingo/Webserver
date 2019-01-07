package edu.udo.cs.rvs;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import edu.udo.cs.rvs.client.HttpClient;

/**
 * Nutzen Sie diese Klasse um den HTTP Server zu implementieren. Sie duerfen
 * weitere Klassen erstellen, sowie Klassen aus den in der Aufgabe aufgelisteten
 * Paketen benutzen. Achten Sie darauf, Ihren Code zu dokumentieren und moegliche
 * Ausnahmen (Exceptions) sinnvoll zu behandeln.
 * 
 * @author Felix Obenaus (Matrikelnr. 205637)
 * @author Abdulhamed Chribati (Matrikelnr. 206317)
 */
public class HttpServer
{
	private ServerSocket server;
	private boolean running;
	private ArrayList<HttpClient> clients = new ArrayList<>();
	
    /**
     * Beispiel Dokumentation fuer dieses Attribut:
     * Dieses Attribut gibt den Basis-Ordner fuer den HTTP-Server an.
     */
    public static final File wwwroot = new File("wwwroot");
    
    /**
     * Der Port, auf dem der HTTP-Server lauschen soll.
     */
    private int port;

    /**
     * Beispiel Dokumentation fuer diesen Konstruktor:
     * Der Server wird initialisiert und der gewuenschte Port
     * gespeichert.
     * 
     * @param port
     *            der Port auf dem der HTTP-Server lauschen soll
     */
    public HttpServer(int port)
    {
        this.port = port;
    }
    
    public void stop() {
    	this.running=false;
    }
    
    public void remove(HttpClient client) {
    	this.clients.remove(client);
    }
    
    /**
     * Beispiel Dokumentation fuer diese Methode:
     * Diese Methode oeffnet einen Port, auf dem der HTTP-Server lauscht.
     * Eingehende Verbindungen werden in einem eigenen Thread behandelt.
     */
    public void startServer()
    {
    	try {
    		System.out.println("HTTPServer started");
    		this.running=true;
			this.server = new ServerSocket(this.port);
			System.out.println("Server is listing on port "+this.port);
			
			while(this.running) {
				Socket socket = this.server.accept();
				this.clients.add(new HttpClient(this,socket));
			}
			
			this.server.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}
