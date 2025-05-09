package com.chat_java_tp_client.helpers;

import org.json.JSONObject;

public class User {
	private int idUser;
	private String firstname;
	private String lastname;
	private String username;
	private String password;
	private String dateAdd;
	private int isLogged;
	private String sexe; // M, F
 

	public static final String sexeM = "M";
	public static final String sexeF = "F";

	// Constructeur complet
	public User(int idUser, String firstname, String lastname, String username, String password, String dateAdd,
			int isLogged) {
		this.idUser = idUser;
		this.firstname = firstname;
		this.lastname = lastname;
		this.username = username;
		this.password = password;
		this.dateAdd = dateAdd;
		this.isLogged = isLogged;
		this.sexe = sexe;
	}

	// Constructeur à partir d'un JSONObject
	public User(JSONObject user) {
		this.idUser = user.optInt("idUser", 0); // Retourne 0 si la clé "idUser" n'existe pas
		this.firstname = user.optString("firstname", null); // Retourne null si la clé "firstname" n'existe pas
		this.lastname = user.optString("lastname", null);
		this.username = user.optString("username", null);
		this.password = user.optString("password", null);
		this.dateAdd = user.optString("dateAdd", null);
		this.isLogged = user.optInt("isLogged", 0);
		this.sexe = user.getString("sexe");
	}

	// Getters et Setters
	public int getIdUser() {
		return idUser;
	}

	public void setIdUser(int idUser) {
		this.idUser = idUser;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDateAdd() {
		return dateAdd;
	}

	public void setDateAdd(String dateAdd) {
		this.dateAdd = dateAdd;
	}

	public int getIsLogged() {
		return isLogged;
	}

	public void setIsLogged(int isLogged) {
		this.isLogged = isLogged;
	}

	public String getSexe() {
		return this.sexe;
	}
 

	@Override
	public String toString() {
		return "User{" + "idUser=" + idUser + ", firstname='" + firstname + '\'' + ", lastname='" + lastname + '\''
				+ ", username='" + username + '\'' + ", password='" + password + '\'' + ", dateAdd='" + dateAdd + '\''
				+ ", isLogged=" + isLogged + '}';
	}
}
