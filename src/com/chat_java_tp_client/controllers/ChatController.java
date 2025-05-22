package com.chat_java_tp_client.controllers;

import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

public class ChatController implements Initializable {

	@FXML
	private GridPane imageContainer;
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
	@FXML
	private HBox footerHbox;
	@FXML
	private HBox HBoxCtrlSend;
	@FXML
	private HBox HBoxCtrlEmoji;

	private ChatApp mainApp;
	private MessageController boxMessage;

	private SocketManagerMessage socketManagerMessage;
	AppState appState;
	private User currentUser;
	private Sound soundApp;
	private boolean isCall = false;
	private User selectedUser;
	AudioCallWindow audioCallWindow;
	VideoCallWindow videoCallWindow;

	Parent loaderMessages;

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
				fileMessage.put("idSend", currentUser.getIdUser());
				fileMessage.put("idUser", currentUser.getIdUser());
				fileMessage.put("idReceive", selectedUser.getIdUser());
				fileMessage.put("type", Helpers.sendFile);

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
			audioCallWindow = new AudioCallWindow(this);
			audioCallWindow.startCallWindow(false);
			Platform.runLater(() -> {
				initBtnCallDisabled(true);
			});
			ServerSocket serverSocketCall = audioCallWindow.getServerCall().getServerSocket_audio();

			JSONObject signal = new JSONObject();
			signal.put("action", Helpers.audioType);
			signal.put("content", "Repond a mon appel");

			signal.put("idSend", currentUser.getIdUser());
			signal.put("idUser", currentUser.getIdUser());
			signal.put("idReceive", selectedUser.getIdUser());
			signal.put("type", Helpers.audioType);
			signal.put("ip", Helpers.getLocalIpAddress());
			signal.put("port", serverSocketCall.getLocalPort());
			boxMessage.addMessage(new Message(signal), currentUser);
			socketManagerMessage.getOutputStream().println(signal.toString());
		}
	}

	@FXML
	private void actionKeyReleased(KeyEvent event) {
		Helpers.enterToAction(event, this::handlerSendMesage);
	}

	public void signalRespCall(String type, String socketAddress, int port, int idReceive, int portVideo) {

		if (socketManagerMessage.getOutputStream() != null) {
			JSONObject signal = new JSONObject();
			signal.put("action", type);
			signal.put("content", "Repond a mon appel reception");
			signal.put("idUser", currentUser.getIdUser());
			signal.put("idSend", currentUser.getIdUser());
			signal.put("idReceive", idReceive);

			signal.put("type", type);
			signal.put("ip", socketAddress);
			signal.put("port", port);
			if (portVideo != 0) {
				signal.put("port_video", portVideo);
			}

			socketManagerMessage.getOutputStream().println(signal.toString());
			System.out.println("signalRespCall : " + signal);
		}
	}

	@FXML
	public void signalVideoCall() {
		if (socketManagerMessage.getOutputStream() != null && selectedUser != null) {
			videoCallWindow = new VideoCallWindow(this);
			videoCallWindow.startCallWindow(false);
			Platform.runLater(() -> {
				initBtnCallDisabled(true);
			});
			ServerSocket serverAudioSocketCall = videoCallWindow.getServerCall().getServerSocket_audio();
			ServerSocket serverVideoSocketCall = videoCallWindow.getServerCallVideo().getServerSocket_video();

			JSONObject signal = new JSONObject();
			signal.put("action", Helpers.videoType);
			signal.put("content", "Repond a mon appel");

			signal.put("idSend", currentUser.getIdUser());
			signal.put("idUser", currentUser.getIdUser());
			signal.put("idReceive", selectedUser.getIdUser());
			signal.put("type", Helpers.videoType);
			signal.put("ip", Helpers.getLocalIpAddress());
			signal.put("port", serverAudioSocketCall.getLocalPort());
			signal.put("port_video", serverVideoSocketCall.getLocalPort());
			boxMessage.addMessage(new Message(signal), currentUser);
			System.out.println(boxMessage);
			socketManagerMessage.getOutputStream().println(signal.toString());

		}
	}

	private void initEmojiBlok(boolean isVisible) {
		HBoxCtrlEmoji.setVisible(isVisible);
		HBoxCtrlEmoji.setManaged(isVisible);
		HBoxCtrlSend.setVisible(!isVisible);
		HBoxCtrlSend.setManaged(!isVisible);
	}

	@FXML
	void closeEmoji(MouseEvent event) {
		initEmojiBlok(false);
	}

	@FXML
	void openEmoji(MouseEvent event) {
		initEmojiBlok(true);
	}

	public void signalAskFile(int id) {
		if (socketManagerMessage.getOutputStream() != null && selectedUser != null) {

			JSONObject signal = new JSONObject();
			signal.put("action", Helpers.askFile);
			signal.put("content", "");

			signal.put("idSend", currentUser.getIdUser());
			signal.put("idUser", currentUser.getIdUser());
			signal.put("idMessage", id);
			signal.put("idReceive", currentUser.getIdUser());
			signal.put("type", Helpers.askFile);
			socketManagerMessage.getOutputStream().println(signal.toString());
		}
	}

	public void signalSendEmoji(String name) {
		if (socketManagerMessage.getOutputStream() != null && selectedUser != null) {
			JSONObject signal = new JSONObject();
			signal.put("action", Helpers.emoji);
			signal.put("content", "");
			signal.put("fileName", name);

			signal.put("idSend", currentUser.getIdUser());
			signal.put("idUser", currentUser.getIdUser());
			signal.put("idReceive", selectedUser.getIdUser());
			signal.put("type", Helpers.emoji);
			addContentMessageListview(signal);
			socketManagerMessage.getOutputStream().println(signal);
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
		this.selectedUser = appState.getSelecetedUser();
	}

	public void setCurrentUser(User user) {
		this.currentUser = user;
	}

	public void addContentMessageListview(JSONObject data) {

		System.out.println("jsonObjectjsonObject : " + data);
		if (data.optString("usernameSend", null) == null && data.optString("username", null) == null) {
			int idUser = data.optInt("idUser", data.optInt("idSend", 0));
			if (idUser != 0) {
				for (int i = 0; i < appState.getAllUsers().length(); i++) {
					JSONObject user = appState.getAllUsers().getJSONObject(i);
					if (user.optInt("idUser", 0) == idUser) {
						data.put("usernameSend", user.optString("username", "Unknown"));
						break;
					}
				}
			}
		}
		Message mess = new Message(data);
		boxMessage.addMessage(mess, currentUser);
		scrollDownMessage();
	}

	private void scrollDownMessage() {
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

	public void initBtnCallDisabled(boolean isCall) {
		isCall = isCall;
		callBtnVideo.setDisable(true);
		callBtnAudio.setDisable(true);
	}

	public Parent getRootView() {
		return sendButton.getScene().getRoot();
	}

	public void handlerSendMesage() {
		String message = inputField.getText();
		User selectedUser = appState.getSelecetedUser();
		if (!message.isEmpty() && socketManagerMessage != null && selectedUser != null) {
			JSONObject messageDatas = new JSONObject();
			messageDatas.put("idUser", currentUser.getIdUser());
			messageDatas.put("idSend", currentUser.getIdUser());
			messageDatas.put("idReceive", selectedUser.getIdUser());
			messageDatas.put("content", message);
			messageDatas.put("type", "simple");
			socketManagerMessage.getOutputStream().println(messageDatas.toString()); // Envoi du message au serveur
			addContentMessageListview(messageDatas);
			inputField.clear();
		}
	}

	public void listentServerIn(JSONObject jsonObject) {
		String action = jsonObject.getString("action");
		if (Helpers.askFile.equals(action)) {
			JSONObject fileData = jsonObject.getJSONObject("datas");
			String fileName = fileData.getString("fileName");
			byte[] fileContent = Base64.getDecoder().decode(fileData.getString("fileContent"));

			File downloadedFile = new File(Helpers.FILE_DOWNLOAD + fileName);
			downloadedFile.getParentFile().mkdirs(); // Créer le dossier si nécessaire

			if (downloadedFile.exists()) {
				// Si le fichier existe déjà
				Platform.runLater(() -> {
					Helpers.showMessage("SendFile", "Fichier déjà reçu : " + fileName, "success");
					Helpers.openFileFolder(Helpers.FILE_DOWNLOAD);
				});
			} else {
				// Si le fichier n'existe pas encore
				try (FileOutputStream fos = new FileOutputStream(downloadedFile)) {
					fos.write(fileContent);
					Platform.runLater(() -> {
						Helpers.showMessage("SendFile", "Fichier reçu : " + fileName + " (enregistré dans downloads)\n",
								"success");
						Helpers.openFileFolder(Helpers.FILE_DOWNLOAD);
					});
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else if (Helpers.sendFile.equals(action)) {
			JSONObject mess = jsonObject.getJSONObject("datas");
			addContentMessageListview(mess);
		} else if (Helpers.emoji.equals(action)) {
			JSONObject mess = jsonObject.getJSONObject("datas");
			if (appState.getSelecetedUser() != null
					&& appState.getSelecetedUser().getIdUser() == mess.getInt("idSend")) {
				addContentMessageListview(mess);
			}
			soundApp.playSound(Sound.NOTIFICATION, false);
		} else if (Helpers.audioType.equals(action)) {
			if (isCall) {
				return;
			}
			audioCallWindow = new AudioCallWindow(this);
			String ip = jsonObject.getString("ip");
			int port = jsonObject.getInt("port");
			int id_caller = jsonObject.getInt("idUser");
			audioCallWindow.setIp_come(ip);
			audioCallWindow.setPort_come(port);
			audioCallWindow.setId_caller(id_caller);
			Platform.runLater(() -> {
				audioCallWindow.startCallWindow(true);
				initBtnCallDisabled(true);
			});

		} else if (Helpers.audioTypeReceiver.equals(action)) {
			if (audioCallWindow == null) {
				return;
			}
			Platform.runLater(() -> {
				initBtnCallDisabled(true);
			});
			String ip = jsonObject.getString("ip");
			int port = jsonObject.getInt("port");
			audioCallWindow.setIp_come(ip);
			audioCallWindow.setPort_come(port);
			// reception
			audioCallWindow.getReceiveCall().start(ip, port);

		} else if (Helpers.videoType.equals(jsonObject.get("action"))) {
			if (isCall) {
				return;
			}
			videoCallWindow = new VideoCallWindow(this);
			String ip = jsonObject.getString("ip");
			int port = jsonObject.getInt("port");
			int port_video = jsonObject.getInt("port_video");
			int id_caller = jsonObject.getInt("idUser");
			videoCallWindow.setIp_come(ip);
			videoCallWindow.setPort_come(port);
			videoCallWindow.setPort_come_video(port_video);
			videoCallWindow.setId_caller(id_caller);
			Platform.runLater(() -> {
				videoCallWindow.startCallWindow(true);
				initBtnCallDisabled(true);
			});

		} else if (Helpers.videoTypeReceiver.equals(action)) {
			if (videoCallWindow == null) {
				return;
			}
			Platform.runLater(() -> {
				initBtnCallDisabled(true);
			});
			String ip = jsonObject.getString("ip");
			int port = jsonObject.getInt("port");
			int port_video = jsonObject.getInt("port_video");
			videoCallWindow.setIp_come(ip);
			videoCallWindow.setPort_come(port);
			videoCallWindow.setPort_come_video(port_video);
			// reception
			videoCallWindow.getReceiveCall().start(ip, port);
			videoCallWindow.getReceiveCallVideo().start(ip, port_video);

		} else if (Helpers.otherUserLogged.equals(action)) {
			JSONObject mess = jsonObject.getJSONObject("datas");
			Boolean success = jsonObject.getBoolean("success");

			User newCurrentUser = new User(mess.getJSONObject("user"));
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
					e.printStackTrace();
				}
			});

		} else if (Helpers.responseSendMessage.equals(action)) {
			JSONObject datas = jsonObject.getJSONObject("datas");
		} else if (Helpers.getMessUserSendReceive.equals(jsonObject.get("action"))) {
			JSONObject datas = jsonObject.getJSONObject("datas");
			JSONArray mess = datas.optJSONArray("messages");
			boolean success = jsonObject.getBoolean("success");
			if (success) {
				Platform.runLater(() -> {
					contentMessageListView.getItems().clear();
					loadMessages(mess);
				});

			} else {
			}
		} else {
			soundApp.playSound(Sound.NOTIFICATION, false);
			JSONObject mess = jsonObject.getJSONObject("datas"); 
			if (appState.getSelecetedUser() != null
					&& appState.getSelecetedUser().getIdUser() == mess.getInt("idSend")) {
				addContentMessageListview(mess);
			}
		}

	}

	public void firstConnection() {
		if (currentUser != null) {
			CurrentUsername.setText(currentUser.getUsername());
		}

		// Récupérer le conteneur Users principal dans FXML
		boxMessage = new MessageController(contentMessageListView, this);
//		loadMessages(appState.getOldmessages());
		updateUserBlock();
	}

	private void loadMessages(JSONArray jsonArray) {
		boxMessage.removeAllChild();
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject message = jsonArray.getJSONObject(i);
			boxMessage.addMessage(new Message(message), currentUser);
		}
	}

	public void updateUserBlock() {
		if (appState.getSelecetedUser() != null) {
			selectedUser = appState.getSelecetedUser();
		}
		contentUsersListView.getItems().clear();
		for (int i = 0; i < appState.getAllUsers().length(); i++) {
			JSONObject user = appState.getAllUsers().getJSONObject(i);
			User newU = new User(user);
			if (newU.getIdUser() != currentUser.getIdUser()) {
				FXMLLoader loader = new FXMLLoader(
						getClass().getResource(Helpers.getResourcesPath() + "fxml/ui/userBlock.fxml"));
				try {
					Parent userBlock = loader.load();
					UserBlocController userBlocController = loader.getController();
					userBlocController.setMainApp(mainApp);
					userBlocController.setChatController(this);
					userBlocController.setAppState(appState);
					userBlocController.setUser(newU);

					if (newU.getSexe().equals(User.sexeF)) {
						userBlock.getStyleClass().add("userFBlock");
					}
					if (selectedUser != null && selectedUser.getIdUser() == newU.getIdUser()) {
						userBlock.getStyleClass().add("active_user");
					}
					if (newU.getIsLogged() != 0) {
						userBlock.getStyleClass().add("statusOnline");
					} else {
						userBlock.getStyleClass().add("statusOffline");
					}
					contentUsersListView.getItems().add(userBlock);

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}

	}

	public AppState getAppState() {
		return appState;
	}

	public void setAppState(AppState appState) {
		this.appState = appState;
	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		try {
			FXMLLoader loader = new FXMLLoader(
					getClass().getResource(Helpers.getResourcesPath() + "fxml/component/loader.fxml"));
			loaderMessages = loader.load();
			noSelectUser();
			try {
				File dir = new File(Helpers.imageDirectoryPathEmoji);
//				System.out.println("imageDirectoryPath : " + Helpers.imageDirectoryPathEmoji + " -> " + dir);

				if (dir.exists() && dir.isDirectory()) {
					File[] imageFiles = dir.listFiles(
							(d, name) -> name.toLowerCase().matches(".*\\.(png|jpg|jpeg|gif)") && !name.contains("-"));

					if (imageFiles != null) {
						int columns = 10;
						int row = 0;
						int col = 0;

						for (File file : imageFiles) {
							try {
								// Redimensionnement dès l'import pour limiter l’usage mémoire
								Image image = new Image(new FileInputStream(file), 100, 100, true, true);
								ImageView imageView = new ImageView(image);
								imageView.setFitWidth(50);
								imageView.setFitHeight(50);
								imageView.setPreserveRatio(true);
								imageView.setSmooth(true);

								imageView.setOnMouseClicked(e -> {
									signalSendEmoji(file.getName());
								});

								imageContainer.add(imageView, col, row);

								col++;
								if (col == columns) {
									col = 0;
									row++;
								}

							} catch (FileNotFoundException e) {
								System.err.println("Image introuvable : " + file.getName());
								e.printStackTrace();
							}
						}
					}
				} else {
					System.err.println("Le dossier spécifié n'existe pas ou n'est pas un dossier.");
				}
			} catch (Exception e) {
				System.err.println("Erreur lors du chargement des images : ");
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void noSelectUser() {
		try {
			FXMLLoader noSelectU = new FXMLLoader(
					getClass().getResource(Helpers.getResourcesPath() + "fxml/component/noSelectedUser.fxml"));
			Parent l = noSelectU.load();
			Platform.runLater(() -> {
				contentMessageListView.getItems().clear();
				contentMessageListView.getItems().add(l);
				initBtnCallDisabled(false);
				footerHbox.setVisible(false);
				footerHbox.setManaged(false);
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setMainApp(ChatApp chatApp) {
		this.mainApp = chatApp;
		this.appState = chatApp.getAppState();
		this.socketManagerMessage = chatApp.getSocketManagerMessage();
		this.soundApp = chatApp.getSoundApp();
		firstConnection();
	}

	public void ChangeUser(User userSelected) {
		handleEndCall();
		updateUserBlock();
		footerHbox.setVisible(true);
		footerHbox.setManaged(true);
		initEmojiBlok(false);
		contentMessageListView.getItems().clear();
		contentMessageListView.getItems().add(loaderMessages);

		JSONObject messageDatas = new JSONObject();
		messageDatas.put("idUser", currentUser.getIdUser());
		messageDatas.put("idReceive", userSelected.getIdUser());
		messageDatas.put("action", Helpers.getMessUserSendReceive);
		socketManagerMessage.getOutputStream().println(messageDatas.toString());
		scrollDownMessage();
	}

}
