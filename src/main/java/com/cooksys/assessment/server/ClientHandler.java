package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Client;
import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler extends Thread implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);

	private Socket socket;
	private String userName;
	// threads represents multiple connections
	private final ClientHandler[] threads;
	private int maxClientsCount;
	private PrintWriter writer = null;
	private ObjectMapper mapper;
	private Message previousMsg;

	public ClientHandler(Socket socket, ClientHandler[] threads) {
		super();
		this.socket = socket;
		this.threads = threads;
	    maxClientsCount = threads.length;
	}

	public void run() {
		try {
			mapper = new ObjectMapper();
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
			
			while (!socket.isClosed()) {
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);

				processMessage(message);
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}
	
	///
	private void processMessage(Message message){		
		try{
			switch (message.getCommand().toLowerCase()) {
				case "connect":
					connectCommand(message);
					break;
				case "disconnect":
					disconnectCommand(message);
					this.socket.close();
					break;
				case "echo":
					echoCommand(message);
					previousMsg = message;
					break;
				case "users":
					userCommand(message);
					previousMsg = message;
					break;
				case "username":
					sendMessage(message);
					previousMsg = message;
					break;
				case "broadcast":
					broadcastCommand(message);
					previousMsg = message;
					break;
				default:
					if(previousMsg == null)
						noCommand(message);
					else{
						message.setContents(message.getCommand()+ " "+message.getContents());
						message.setCommand(previousMsg.getCommand());
						processMessage(message);
					}
					break;
			}
		}
		catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}
	
	private String createMessage(String msg){
		return new java.util.Date().toString() + ": " + msg;
	}
	
	private void connectCommand(Message message) throws JsonProcessingException {
		log.info("user <{}> connected", message.getUsername());
		userName = message.getUsername();
		boolean threadSet = false;
		for (int i = 0; i < maxClientsCount; i++) {
			if(!threadSet && threads[i] == null){
				threads[i] = this;
				threadSet = true;
			}
			// set the current thread to show that another client has joined
	        if (threads[i] != null && threads[i] != this) {
	        	message.setContents(createMessage(message.getUsername() + " has connected"));
	          threads[i].writer.write(mapper.writeValueAsString(message));
	          threads[i].writer.flush();
	        }
	      }
	}
	
	/*
     * Set the current thread variable to null so that a new client
     * could be accepted by the server.
     */
	private void disconnectCommand(Message message) throws JsonProcessingException {
		log.info("user <{}> disconnected", message.getUsername());
	      for (int i = 0; i < maxClientsCount; i++) {
	        if (threads[i] == this) {
	          threads[i] = null;
	        }
	        else{
	        	// show when someone disconnects
	        	if (threads[i] != null && threads[i] != this) {
		        	message.setContents(createMessage(message.getUsername() + " has disconnected"));
		          threads[i]. writer.write(mapper.writeValueAsString(message));
		          threads[i]. writer.flush();
		        }
	        }
	    }		
	}

	private void echoCommand(Message message) throws JsonProcessingException {
		log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
		String response = mapper.writeValueAsString(message);
		writer.write(response);
		writer.flush();
	}
	
	private void noCommand(Message message) throws JsonProcessingException {
		message.setContents(createMessage("command is required"));
		String response = mapper.writeValueAsString(message);
		writer.write(response);
		writer.flush();
	}
		
	private void userCommand(Message message) throws JsonProcessingException {
		log.info("request users");
		StringBuilder sb = new StringBuilder();
		String timeStamp = new java.util.Date().toString();
		sb.append(timeStamp +": currently connected users:");
		for (int i = 0; i < maxClientsCount; i++) {
	        if (threads[i] != null) {
	        	sb.append("\n"+threads[i].userName);
	        }
		}		
		message.setContents(sb.toString());
		writer.write(mapper.writeValueAsString(message));	
		writer.flush();
	}
	
	private void broadcastCommand(Message message) throws JsonProcessingException{
		for (int i = 0; i < maxClientsCount; i++) {
	        if (threads[i] != null && !threads[i].userName.equals(message.getUsername())) {
	        	message.setContents(createMessage(message.getContents()));
				threads[i].writer.write(mapper.writeValueAsString(message));	
				threads[i].writer.flush();
	        }
		}
	}
	
	private void sendMessage(Message message) throws JsonProcessingException{
		String content = message.getContents();
		String user = content.substring(0, content.indexOf(" "));
		for (int i = 0; i < maxClientsCount; i++) {
	        if (threads[i] != null && threads[i].userName.equals(user)) {
	        	message.setContents(createMessage(content.substring(content.indexOf(" "))));
				threads[i].writer.write(mapper.writeValueAsString(message));	
				threads[i].writer.flush();
	        }
		}
	}
}