package com.chat_java_tp_client.helpers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.json.JSONObject;

public class Message {
	private String dateAdd;
	private int idMessage;
	private int idSend;
	private int idReceive;
	private String type;
	private String content;
	private String fileName;
	private String usernameSend;

	// Constructeur, getters, et setters
	public Message(String dateAdd, int idMessage, int idSend, int idReceive, String type, String content,
			String fileName) {
		this.dateAdd = dateAdd;
		this.idMessage = idMessage;
		this.idSend = idSend;
		this.idReceive = idReceive;
		this.type = type;
		this.content = content;
		this.fileName = fileName;
	}

	public Message(JSONObject message) {
		// Vérifie et initialise chaque attribut si la clé existe
		this.dateAdd = message.optString("dateAdd", new Date().toString()); // Retourne null si la clé n'existe pas
		this.idMessage = message.optInt("idMessage", 0); // Retourne 0 si la clé n'existe pas
		this.idSend = message.optInt("idSend", 0);
		this.idReceive = message.optInt("idReceive", 0);
		this.type = message.optString("type", "simple");
		this.content = message.optString("content", null);
		this.fileName = message.optString("fileName", null);
		this.usernameSend = (message.optString("usernameSend", null) != null) ? message.optString("usernameSend", null)
				: message.optString("username", null);
	}

	public String getDateAdd() {
		return dateAdd;
	}

	public int getIdMessage() {
		return idMessage;
	}

	public int getIdSend() {
		return idSend;
	}

	public int getIdReceive() {
		return idReceive;
	}

	public String getType() {
		return type;
	}

	public String getContent() {
		return content;
	}

	public String getFileName() {
		return fileName;
	}

	public String getUsernameSend() {
		return usernameSend;
	}

	@Override
	public String toString() {
		return "Message{" + "idMessage=" + idMessage + ", idSend=" + idSend + ", idReceive=" + idReceive + ", fileName=" + fileName + ", content='"
				+ content + '\'' + '}';
	}
}
