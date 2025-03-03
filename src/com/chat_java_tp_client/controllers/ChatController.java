package com.chat_java_tp_client.controllers;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Base64;
import java.util.ResourceBundle;

import org.json.JSONArray;
import org.json.JSONObject;

import com.chat_java_tp_client.ChatApp;
import com.chat_java_tp_client.AudioCallWindow.AudioCallWindow;
import com.chat_java_tp_client.VideoCallWindow.VideoCallWindow;
import com.chat_java_tp_client.helpers.AppState;
import com.chat_java_tp_client.helpers.Helpers;
import com.chat_java_tp_client.helpers.Message;
import com.chat_java_tp_client.helpers.SocketManagerMessage;
import com.chat_java_tp_client.helpers.User;
import com.chat_java_tp_client.sound.Sound;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;

public class ChatController implements Initializable {

	@FXML
	private ListView contentMessageListView;
	@FXML
	private ListView contentUsersListView;
	@FXML
	private TextArea inputField;
	@FXML
	private Button callBtnAudio;
	@FXML
	private Button callBtnVideo;
	@FXML
	private Button sendButton;
	@FXML
	private Button fileButton;
	@FXML
	private Button logoutBtn;
	@FXML
	private Label CurrentUsername;

	private ChatApp mainApp;
	private MessageController boxMessage;

	private SocketManagerMessage socketManagerMessage;
	AppState appState;
	private User currentUser;
	private Sound soundApp;
	private boolean isCall = false;

	@FXML
	public void handleActionLogout(ActionEvent event) {
		logoutBtn.setText("En cours ...");
		JSONObject signal = new JSONObject();
		signal.put("action", Helpers.logout);
		signal.put("idUser", currentUser.getIdUser());
		socketManagerMessage.getOutputStream().println(signal.toString());
	}

	@FXML
	public void sendMessage(ActionEvent event) {
		handlerSendMesage();
	}

	@FXML
	public void sendFile(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();
		File file = fileChooser.showOpenDialog(null);

		if (file != null && file.exists()) {
			try (FileInputStream fis = new FileInputStream(file)) {
				byte[] fileBytes = fis.readAllBytes();
				JSONObject fileMessage = new JSONObject();
				fileMessage.put("action", "send_file");
				fileMessage.put("fileName", file.getName());
				fileMessage.put("fileSize", fileBytes.length);
				fileMessage.put("fileContent", Base64.getEncoder().encodeToString(fileBytes));

				socketManagerMessage.getOutputStream().println(fileMessage.toString()); // Envoi du fichier au serveur
				Helpers.showMessage("SendFile", "Fichier Envoyer", "success");
//				addContentMessageListview(fileMessage);
//				messageArea.appendText("Fichier envoyé : " + file.getName() + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@FXML
	public void signalAudioCall(ActionEvent event) {
		if (socketManagerMessage.getOutputStream() != null) {
			JSONObject signal = new JSONObject();
			signal.put("action", Helpers.audioType);
			signal.put("content", "Repond a mon appel");
			
			signal.put("type", Helpers.audioType);
			boxMessage.addMessage(new Message(signal), currentUser);
//			socketManagerMessage.getOutputStream().println(signal.toString());
//			Platform.runLater(() -> {
//				AudioCallWindow audioCallWindow = new AudioCallWindow(this);
//				audioCallWindow.startCallWindow(false);
//				initBtnCallDisabled();
//			});
			
		}
	}

	@FXML
	private void actionKeyReleased(KeyEvent event) {
		Helpers.enterToAction(event, this::handlerSendMesage);
	}

	@FXML
	public void signalVideoCall() {
		if (socketManagerMessage.getOutputStream() != null) {
			JSONObject signal = new JSONObject();
			signal.put("action", Helpers.videoType);
			signal.put("content", "Repond à mon appel vidéo");
			socketManagerMessage.getOutputStream().println(signal.toString());

			// Affichage de la fenêtre vidéo
			Platform.runLater(() -> {
				VideoCallWindow videoCallWindow = new VideoCallWindow(this);
				videoCallWindow.startCallWindow(true);
				initBtnCallDisabled();
			});
		}
	}

	// Getter
	public Button getCallBtnAudio() {
		return callBtnAudio;
	}

	public Button getCallBtnVideo() {
		return callBtnVideo;
	}

	public Button getSendButton() {
		return sendButton;
	}

	public Button getFileButton() {
		return fileButton;
	}

	public void setSocketManagerMessage(SocketManagerMessage socketManagerMessage) {
		this.socketManagerMessage = socketManagerMessage;
	}

	public TextArea getInputField() {
		return inputField;
	}

	public AppState getAppSte() {
		return this.appState;
	}

	// Setter

	public void setAppSte(AppState appState) {
		this.appState = appState;
		this.currentUser = appState.getCurrentUser();
	}

	public void setCurrentUser(User user) {
		this.currentUser = user;
	}

	public void addContentMessageListview(JSONObject data) {
		MessageController boxMessage = new MessageController(contentMessageListView);
		if (data.optString("usernameSend", null) == null && data.optString("username", null) == null) {
			int idSend = data.optInt("idSend", 0);
			if (idSend != 0) {
				for (int i = 0; i < appState.getAllUsers().length(); i++) {
					JSONObject user = appState.getAllUsers().getJSONObject(i);
					if (user.optInt("idUser", 0) == idSend) {
						data.put("usernameSend", user.optString("username", "Unknown"));
						break;
					}
				}
			}
		}

		boxMessage.addMessage(new Message(data), currentUser);
		Platform.runLater(() -> {
			if (contentMessageListView != null) {
				contentMessageListView.scrollTo(contentMessageListView.getItems().size() - 1);
			}
		});

	}

	public void handleEndCall() {
		isCall = false;
		callBtnVideo.setDisable(false);
		callBtnAudio.setDisable(false);
	}

	public void initBtnCallDisabled() {
		isCall = true;
		callBtnVideo.setDisable(true);
		callBtnAudio.setDisable(true);
	}

	public Parent getRootView() {
		return sendButton.getScene().getRoot();
	}

	public void handlerSendMesage() {
		String message = inputField.getText();
		if (!message.isEmpty() && socketManagerMessage != null) {
			JSONObject messageDatas = new JSONObject();
			messageDatas.put("idSend", currentUser.getIdUser());
			messageDatas.put("idReceive", 2);
			messageDatas.put("content", message);
			messageDatas.put("type", "simple");
			socketManagerMessage.getOutputStream().println(messageDatas.toString()); // Envoi du message au serveur
			addContentMessageListview(messageDatas);
			inputField.clear();
		}
	}

	public void listentServerIn(JSONObject jsonObject) {
//		contentMessageListView.getItems().removeAll();
		// Thread pour écouter les messages entrants
		String action = jsonObject.getString("action");
		if ("get_messages".equals(action)) {

		}

		if (Helpers.sendFile.equals(action)) {
			soundApp.playSound(Sound.NOTIFICATION, false);
			JSONObject fileData = jsonObject.getJSONObject("datas");
			String fileName = fileData.getString("fileName");
			byte[] fileContent = Base64.getDecoder().decode(fileData.getString("fileContent"));

			File downloadedFile = new File(Helpers.FILE_DOWNLOAD + fileName);
			downloadedFile.getParentFile().mkdirs();// Créer le dossier si nécessaire

			try (FileOutputStream fos = new FileOutputStream(downloadedFile)) {
				fos.write(fileContent);
				Helpers.showMessage("SendFile", "Fichier reçu : " + fileName + " (enregistré dans downloads)\n",
						"success");
//										messageArea.appendText("Fichier reçu : " + fileName + " (enregistré dans downloads)\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (Helpers.audioType.equals(jsonObject.get("action"))) {
			if (isCall) {
				return;
			}
			JSONObject mess = jsonObject.getJSONObject("datas");
			addContentMessageListview(mess);
//									messageArea.appendText("Serveur: " + mess.getString("content") + "\n");
			Platform.runLater(() -> {
				AudioCallWindow audioCallWindow = new AudioCallWindow(this);
				audioCallWindow.startCallWindow(true);
				initBtnCallDisabled();
			});

		} else if (Helpers.videoType.equals(jsonObject.get("action"))) {
			if (isCall) {
				return;
			}
			JSONObject mess = jsonObject.getJSONObject("datas");
			addContentMessageListview(mess);
//									messageArea.appendText("Serveur: " + mess.getString("content") + "\n");
			Platform.runLater(() -> {
				VideoCallWindow videoCallWindow = new VideoCallWindow(this);
				videoCallWindow.startCallWindow(false);
				initBtnCallDisabled();
			});
		} else if (Helpers.otherUserLogged.equals(jsonObject.get("action"))) {
			JSONObject mess = jsonObject.getJSONObject("datas");
			Boolean success = jsonObject.getBoolean("success");

			JSONArray userArr = mess.getJSONArray("user");
			User newCurrentUser = new User(userArr.getJSONObject(0));
			JSONArray allUsers = mess.getJSONArray("all_users");

			Platform.runLater(() -> {
				try {
					// Mettre à jour l'état de l'application
					if (currentUser.getIdUser() == newCurrentUser.getIdUser()) {
						appState = new AppState();
						mainApp.loadLoginView();
					} else {
						appState.setAllUsers(allUsers);
						mainApp.setAppSte(appState);
						updateUserBlock();
					}

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});

		} else {
			soundApp.playSound(Sound.NOTIFICATION, false);
			JSONObject mess = jsonObject.getJSONObject("datas");
			System.out.println(mess);
			addContentMessageListview(mess);
		}

	}

	public void firstConnection() {
		System.out.println(currentUser);
		System.out.println(CurrentUsername);
		if (currentUser != null) {
			CurrentUsername.setText(currentUser.getUsername());
		}

		// Récupérer le conteneur Users principal dans FXML
		boxMessage = new MessageController(contentMessageListView);
		boxMessage.removeAllChild();
		for (int i = 0; i < appState.getOldmessages().length(); i++) {
			JSONObject message = appState.getOldmessages().getJSONObject(i);
			System.out.println(message);
			boxMessage.addMessage(new Message(message), currentUser);
		}
		updateUserBlock();
	}

	private void updateUserBlock() {
		UserBlocController boxUser = new UserBlocController(contentUsersListView);
		boxUser.removeAllChild();
		for (int i = 0; i < appState.getAllUsers().length(); i++) {
			JSONObject user = appState.getAllUsers().getJSONObject(i);
			User newU = new User(user);
			if (newU.getIdUser() != currentUser.getIdUser()) {
				boxUser.addUser(newU);
			}
		}

	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
//		listentServerIn();
		System.out.println("AudioCallController initialisé !");
	}

	public void setMainApp(ChatApp chatApp) {
		this.mainApp = chatApp;
		this.appState = chatApp.getAppState();
		this.socketManagerMessage = chatApp.getSocketManagerMessage();
		this.soundApp = chatApp.getSoundApp();
		firstConnection();
	}

}
