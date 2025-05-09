package com.chat_java_tp_client.helpers;

import org.json.JSONArray;

public class AppState {
	private static AppState instance;
	private User currentUser;
	private JSONArray allUsers;
	private JSONArray oldmessages;
	private User selecetedUser;

	public AppState() {
		// Initialisation des données globales
	}

	public static AppState getInstance() {
		if (instance == null) {
			instance = new AppState();
		}
		return instance;
	}

	public User getCurrentUser() {
		return this.currentUser;
	}

	public void setCurrentUser(User user) {
		this.currentUser = user;
	}

	public JSONArray getAllUsers() {
		return this.allUsers;
	}

	public void setAllUsers(JSONArray users) {
		this.allUsers = users;
	}

	public void setOldmessages(JSONArray messages) {
		this.oldmessages = messages;
	}

	public JSONArray getOldmessages() {
		return this.oldmessages;
	}

	public User getSelecetedUser() {
		return selecetedUser;
	}

	public void setSelecetedUser(User selecetedUser) {
		this.selecetedUser = selecetedUser;
	}
	
	
}