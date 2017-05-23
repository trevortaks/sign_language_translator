/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

/*
 * FileStreamer.java
 *
 * Created on August 2, 2002, 5:31 AM
 * 
 * (c) UEA, Norwich, UK
 */

/**
 *
 * @author  Robert Smith, UEA (a165905)
 */

import java.util.StringTokenizer;

public class FileStreamer {
    
      
    /** Creates a new instance of FileStreamer */
    public FileStreamer() {
    }
   
    //For transmitting SiGML formatted string to port
    public static void stream(String sigml, String host, int port){
        try {            
            //open a socket connection to the BAF player server
            Socket server = null;
            try {
                server = new Socket(host, port);
            } catch (UnknownHostException u){
                u.printStackTrace();
            } catch (java.net.ConnectException c){
                c.printStackTrace();
            }

            //point a writer to the socket connection
            OutputStream os = null;
            StringTokenizer tokenizer = new StringTokenizer(sigml, "\n");
            String token = new String(tokenizer.nextToken());
            
            os = server.getOutputStream ();

            PrintWriter sockout;
            sockout = new PrintWriter (os, true); // true for auto-flush        
            
            while(tokenizer.hasMoreTokens()){
                //send the data to the player
                System.out.println(token);
                sockout.println(token);
                token = tokenizer.nextToken();
            }
            
            sockout.close();
        } catch (IOException i){
            i.printStackTrace();
        } 
    }
        
    //For transmitting a SiGML file to player   
    public static void stream(File file, String host, int port){
         
        try {
            //point a new reader to the file
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            } catch (FileNotFoundException f){
                f.printStackTrace();
            }

            //open a socket connection to the BAF player server
            Socket server = null;
            try {
                server = new Socket(host, port);
            } catch (UnknownHostException u){
                u.printStackTrace();
            }

            //point a writer to the socket connection
            OutputStream os = null;

                os = server.getOutputStream ();


            PrintWriter sockout;
            sockout = new PrintWriter (os, true); // true for auto-flush        

            String next_line = new String();
            
            //send the data to the BAF player
            while(in.ready()){
                next_line = in.readLine();
                System.out.println(next_line);
                sockout.println(next_line);
            }
            in.close();
            sockout.close();
        } catch (IOException i){
            i.printStackTrace();
        }
 
    }
    
}
