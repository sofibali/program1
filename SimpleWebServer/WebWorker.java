/**
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection. 
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring 
* the fact that the entirety of the webserver execution might be handling
* other clients, too. 
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format). 
*
**/

import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.lang.Runnable;
import java.io.*;
import java.util.Date;
import java.text.DateFormat;
import java.util.*;
import java.nio.file.*;

import com.sun.media.jfxmedia.logging.Logger;

public class WebWorker implements Runnable
{

    private Socket socket;
    private String path;
    private String output;
    private int fileStatus;
    private String contentType;
/**
* Constructor: must have a valid open socket
**/
public WebWorker(Socket s)
{
   socket = s;
}

/**
* Worker thread starting point. Each worker handles just one HTTP 
* request and then returns, which destroys the thread. This method
* assumes that whoever created the worker created it with a valid
* open socket object.
**/
public void run()
{
   System.err.println("Handling connection...");
   try {
      InputStream  is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      readHTTPRequest(is);
      contentType = "";
     //change to a variable that can handle different ccontenttypes
      writeHTTPHeader(os,contentType);
   	  writeContent(os, is);
   	  os.flush();
   	  socket.close();
   } catch (Exception e) {
      System.err.println("Output error: "+e);
   }
   System.err.println("Done handling connection.");
   return;
}

/**
* Read the HTTP request header.
**/
private void getContentType(){
	Path true_path = Paths.get(path);
	File file = true_path.toFile();
	
	try{
			contentType = Files.probeContentType(true_path);
	}catch (IOException ex){	
		System.err.println("IOException: "+ex.getMessage());
	}
	return;
}

private void readHTTPRequest(InputStream is)
{
   int line_num = 0;
   String line;
   BufferedReader r = new BufferedReader(new InputStreamReader(is));
   while (true) {
      try {
         while (!r.ready()) Thread.sleep(1);
         line = r.readLine();
         if (line_num ==0) {
	     Scanner input = new Scanner(line);
	     String file_pathArray[] = line.split(" ");
	     path = file_pathArray[1];
	     input.close();
	 }
	 System.err.println("Request line: ("+line+")");
         line_num++;
	 if(!path.equals("/") && !path.equals("")){
	     try{
		 output = new String();
		 File read_in_file = new File(path.substring(1));
		 Scanner input = new Scanner(read_in_file);
		 while(input.hasNext()){
		     output += input.nextLine() + "<br>";
		 }
		 if(output.contains("<cs371date>") || output.contains("<cs371server>")){
		     output = output.replace("<cs371date>",new Date().toString());
		     output = output.replace("<cs371server>", "SofiaBali CS371 server");
		 }
		 input.close();
		 fileStatus = 1;
		 getContentType();
	     }
	     catch (java.io.FileNotFoundException e){
		 fileStatus = 0;
	     }
	 }
	 else{
	     fileStatus = 2;
	 }
	     if (line.length() == 0)
		 break;
     
      } catch (Exception e) {
         System.err.println("Request error: "+e);
         break;
      }
   }
   return;
}

/**
* Write the HTTP header lines to the client network connection.
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
**/
private void writeHTTPHeader(OutputStream os, String contentType) throws Exception
{
   Date d = new Date();
   DateFormat df = DateFormat.getDateTimeInstance();
   df.setTimeZone(TimeZone.getTimeZone("GMT"));
   os.write("HTTP/1.1 200 OK\n".getBytes());
   os.write("Date: ".getBytes());
   os.write((df.format(d)).getBytes());
   os.write("\n".getBytes());
   os.write("Server: Jon's very own server\n".getBytes());
   //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
   //os.write("Content-Length: 438\n".getBytes()); 
   os.write("Connection: close\n".getBytes());
   os.write("Content-Type: ".getBytes());
   //contwnt type needs to be variable
   os.write(contentType.getBytes());
   os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
   return;
}

/**
* Write the data content to the client network connection. This MUST
* be done after the HTTP header has been written out.
* @param os is the OutputStream object to write to
**/
private void writeContent(OutputStream os, InputStream is ) throws Exception
{
    //add FileInputStream
	if (fileStatus == 1){
		FileInputStream read_in = new FileInputStream(path);
		int file_content = read_in.read();
		while (file_content != -1){
			os.write(file_content);
			file_content = read_in.read();
		}
    }
    else if(fileStatus == 2){
	os.write("<html><head</head><body>\n".getBytes());
	os.write("Home".getBytes());
	os.write("</body></html>\n".getBytes());
    }
    else {
	os.write("<html><head</head><body>\n".getBytes());
	os.write("404 page Not Found".getBytes());
	os.write("</body></html>\n".getBytes());
    }
}

} // end class
