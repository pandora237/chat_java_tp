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
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

public class RegisterController implements Initializable {
	enum Sexe {
		M, F
	}

	@FXML
	private TextField firstnameField;

	@FXML
	private TextField lastnameField;

	@FXML
	private PasswordField passwordField;

	@FXML
	private TextField phoneField;

	@FXML
	private ChoiceBox<Sexe> sexeField;
	@FXML
	private TextField usernameField;

	@FXML
	private Button submitBtn;

	private ChatApp mainApp;

	private SocketManagerMessage socketManagerMessage;

	public void setOutputStream() {
	}

	@FXML
	private void actionKeyReleased(KeyEvent event) {
		Helpers.enterToAction(event, this::actionRegister);
	}

	@FXML
	public void handleRegisterChat(ActionEvent event) {
		actionRegister();
	}

	@FXML
	void handleLoginChat(MouseEvent event) {

	}

	private void actionRegister() {
		String username = usernameField.getText().trim();
		String password = passwordField.getText().trim();
		String firstname = firstnameField.getText().trim();
		String lastname = lastnameField.getText().trim();
		String phone = phoneField.getText().trim();
		Sexe sexe = sexeField.getValue();

		if (firstname.isEmpty() || lastname.isEmpty() || phone.isEmpty() || username.isEmpty() || password.isEmpty()
				|| sexe == null) {
			Helpers.showMessage("Erreur", "Tous les champs doivent être remplis.", null);
			return;
		}

		JSONObject registerRequest = new JSONObject();
		registerRequest.put("action", Helpers.register); // Assure-toi que Helpers.register est bien défini

		JSONObject datas = new JSONObject();
		datas.put("username", username);
		datas.put("password", password);
		datas.put("firstname", firstname);
		datas.put("lastname", lastname);
		datas.put("phone", phone);
		datas.put("sexe", sexe.toString());

		registerRequest.put("datas", datas);

		if (socketManagerMessage != null) {
			socketManagerMessage.getOutputStream().println(registerRequest.toString());
		} else {
			Helpers.showMessage("Erreur", "Connexion au serveur non disponible.", null);
		}
	}

	public void listentServerIn(JSONObject jsonObject) {
		if (Helpers.register.equals(jsonObject.get("action"))) {
			JSONObject mess = jsonObject.getJSONObject("datas");
			Boolean success = jsonObject.getBoolean("success");

			if (success) {
				Platform.runLater(() -> {
					try {
						mainApp.loadLoginView();
					} catch (Exception e) {
						e.printStackTrace();
					}
				});

			} else {
				Platform.runLater(() -> {
					Helpers.showMessage("Erreur", "Echec d'inscription", null);
				});
			}
		}

	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		sexeField.getItems().addAll(Sexe.M, Sexe.F);
		sexeField.setValue(Sexe.M);
	}

	public void setMainApp(ChatApp chatApp) {
		this.mainApp = chatApp;
		this.socketManagerMessage = chatApp.getSocketManagerMessage();

	}
}
