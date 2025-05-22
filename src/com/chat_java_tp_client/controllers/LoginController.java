package com.chat_java_tp_client.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import org.json.JSONArray;
import org.json.JSONObject;

import com.chat_java_tp_client.ChatApp;
import com.chat_java_tp_client.helpers.AppState;
import com.chat_java_tp_client.helpers.Helpers;
import com.chat_java_tp_client.helpers.SocketManagerMessage;
import com.chat_java_tp_client.helpers.User;
import com.chat_java_tp_client.sound.Sound;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

public class LoginController implements Initializable {

	@FXML
	private TextField usernameField;

	@FXML
	private PasswordField passwordField; // Changement de TextField à PasswordField pour cacher le mot de passe

	@FXML
	private Button submitBtn;

	private ChatApp mainApp;

	private SocketManagerMessage socketManagerMessage;

	private AppState appState;

	private Sound soundApp;

	public void setOutputStream() {
	}

	@FXML
	private void actionKeyReleased(KeyEvent event) {
		Helpers.enterToAction(event, this::actionLogin);
	}

	@FXML
	public void handleLoginChat(ActionEvent event) {
		actionLogin();
	}

	@FXML
	void handleRegisterChat(MouseEvent event) {
		Platform.runLater(() -> {
			try {
				mainApp.loadRegisterView();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
  
	private void actionLogin() {
		String username = usernameField.getText().trim();
		String password = passwordField.getText().trim();
		if (username.isEmpty()) {
			Helpers.showMessage("Erreur", "Le nom d'utilisateur ne peut pas être vide.", null);
			return;
		}

		if (password.isEmpty()) {
			Helpers.showMessage("Erreur", "Le mot de passe ne peut pas être vide.", null);
			return;
		}

		// Envoyer les données au serveur
		JSONObject loginRequest = new JSONObject();
		loginRequest.put("action", Helpers.login);
		loginRequest.put("username", username);
		loginRequest.put("password", password); // Ajouter le mot de passe

		if (socketManagerMessage != null) {
			socketManagerMessage.getOutputStream().println(loginRequest.toString()); // Envoi des données au serveur
		}
	}

	public void listentServerIn(JSONObject jsonObject) {
		if (Helpers.login.equals(jsonObject.get("action"))) {
			JSONObject mess = jsonObject.getJSONObject("datas");
			Boolean success = jsonObject.getBoolean("success");

			if (success) {
				JSONArray messages = mess.getJSONArray("messages");
				User currentUser = new User(mess.getJSONObject("user"));
				JSONArray allUsers = mess.getJSONArray("all_users");

				Platform.runLater(() -> {
					try {
						// Mettre à jour l'état de l'application
						appState.setAllUsers(allUsers);
						appState.setCurrentUser(currentUser);
						appState.setOldmessages(messages);
						mainApp.setAppSte(appState);
						mainApp.loadChatView();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});

			} else {
				Platform.runLater(() -> {
					Helpers.showMessage("Erreur", "Echec de connexion", null);
				});
			}
		}

	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
	}

	public void setMainApp(ChatApp chatApp) {
		this.mainApp = chatApp;
		this.appState = chatApp.getAppState();
		this.socketManagerMessage = chatApp.getSocketManagerMessage();
		this.soundApp = chatApp.getSoundApp();

	}
}
