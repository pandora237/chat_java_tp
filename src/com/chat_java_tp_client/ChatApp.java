package com.chat_java_tp_client;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import com.chat_java_tp_client.controllers.ChatController;
import com.chat_java_tp_client.controllers.LoginController;
import com.chat_java_tp_client.helpers.AppState;
import com.chat_java_tp_client.helpers.Helpers;
import com.chat_java_tp_client.helpers.SocketManagerMessage;
import com.chat_java_tp_client.helpers.User;
import com.chat_java_tp_client.sound.Sound;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class ChatApp extends Application {

	private SocketManagerMessage socketManagerMessage = new SocketManagerMessage();

	private Sound soundApp;
	private Stage stage;
	private StackPane root;
	private AppState appState = new AppState();
	private String currentPage = "login";
	private LoginController loginController;
	private ChatController chatController;

	public static void main(String[] args) {
		Application.launch(args);
	}

	@Override
	public void start(Stage win) throws Exception {
		AppState appState = new AppState();
		socketManagerMessage.connect();
		soundApp = new Sound();
		this.stage = win;
		root = new StackPane(); // Conteneur racine pour charger différentes vues
		listentServerInApp();
		// Charger la vue de login par défaut
		loadLoginView();

		// Configurer la scène
		Scene scene = new Scene(root, 1024, 700);
		win.setScene(scene);
		win.setTitle("Application Chat");
		win.setResizable(false); 
		win.show();
	}

	public void loadLoginView() throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource(Helpers.getResourcesPath() + "fxml/login.fxml"));
		Parent loginView = loader.load();
		loginController = loader.getController();
		loginController.setMainApp(this);

		root.getChildren().clear();
		root.getChildren().add(loginView);
		currentPage = "login";
	}

	public void loadChatView() throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource(Helpers.getResourcesPath() + "fxml/app.fxml"));
		Parent chatView = loader.load();
		chatController = loader.getController();
		chatController.setAppSte(appState);
		chatController.setMainApp(this);

		root.getChildren().clear();
		root.getChildren().add(chatView);
		currentPage = "app";
	}

	public AppState getAppState() {
		return this.appState;
	}

	public SocketManagerMessage getSocketManagerMessage() {
		return socketManagerMessage;
	}

	public void setAppSte(AppState appState) {
		this.appState = appState;
	}

	public void listentServerInApp() {
		new Thread(() -> {
			try {
				String response;
				while ((response = socketManagerMessage.getInputStream().readLine()) != null) {
					JSONObject jsonObject = new JSONObject(response);
					if (currentPage.equals("login")) {
						loginController.listentServerIn(jsonObject);
					} else {
						chatController.listentServerIn(jsonObject);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				Platform.runLater(() -> {
					Helpers.showMessage("Erreur", "Problème de connexion au serveur.", null);
				});
			}
		}).start();
	}

	// Arrêt propre de l'application
	@Override
	public void stop() throws IOException {
		if (socketManagerMessage != null) {
			socketManagerMessage.close();
		}
	}

	public Sound getSoundApp() {
		return soundApp;
	}

}