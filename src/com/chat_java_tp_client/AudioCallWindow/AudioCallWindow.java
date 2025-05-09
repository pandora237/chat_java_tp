package com.chat_java_tp_client.AudioCallWindow;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.sound.sampled.TargetDataLine;

import com.chat_java_tp_client.ReceiveCall;
import com.chat_java_tp_client.ServerCall;
import com.chat_java_tp_client.controllers.AudioCallController;
import com.chat_java_tp_client.controllers.ChatController;
import com.chat_java_tp_client.helpers.AppState;
import com.chat_java_tp_client.helpers.Helpers;
import com.chat_java_tp_client.sound.Sound;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class AudioCallWindow {

	protected boolean receiveTransmit = false; // utiliser pour activer l'envoie et reception de receveur d'appel

	protected Stage callStage;
	protected static final int bufferSize = 8000;

	protected volatile boolean running = true; // Flag pour indiquer si le client est actif
	protected final ChatController chatController;

	protected AudioCallController callController;

//	protected SocketManagerCall socketManagerCall = new SocketManagerCall();

	protected Sound soundApp;
	protected StackPane root;
	protected AppState appState;
	protected StringBuilder bufferStringBuilderAudio;
	protected ServerCall serverCall = new ServerCall();
	protected ReceiveCall receiveCall = new ReceiveCall();
	protected String ip_come;
	protected int port_come;
	protected int id_caller;

	public AudioCallWindow(ChatController parentWin) {
		this.chatController = parentWin;
		soundApp = new Sound();
	}

	public void startCallWindow(Boolean is_receive) {
		try {
			callStage = new Stage();
			callStage.setTitle("Appel Audio " + (is_receive ? "entrant" : " sortant "));

			FXMLLoader loader = new FXMLLoader(
					getClass().getResource(Helpers.getResourcesPath() + "fxml/call/audioCall.fxml"));

			Parent callView = loader.load();
			callController = loader.getController();
			callController.setMainCallApp(this);

			// Ajouter la vue dans le conteneur principal
			root = new StackPane();
			root.getChildren().add(callView);

			// Configurer et afficher la scène
			Scene scene = new Scene(root, 500, 400);
			callStage.setScene(scene);
			callStage.setOnCloseRequest(event -> {
				endCall();
				event.consume();
			});
			callStage.setResizable(false);
			callStage.show();

			if (is_receive) {
				soundApp.playSound(Sound.CALL_AUDIO, true);
				callController.updateButtonState(false);
			} else {
				sendOrReceive(is_receive, true);
				callController.updateButtonState(true);
			}
		} catch (IOException e) {
			e.printStackTrace();
			// Afficher une alerte en cas d'erreur
			Platform.runLater(() -> {
				Helpers.showMessage("Erreur", "Impossible d'ouvrir la fenêtre d'appel", "");
//				alert.setContentText("Détails : " + e.getMessage()); 
			});
		}
	}

	public void sendOrReceive(boolean is_receive, boolean send_signal_audio) {
		if (is_receive) {
			if (ip_come != null) {
				receiveCall.start(ip_come, port_come);
				// demarrage du server d'envoie
				serverCall.startServer();
				if (send_signal_audio) {
					chatController.signalRespCall(Helpers.audioTypeReceiver, Helpers.getLocalIpAddress(),
							serverCall.getServerSocket_audio().getLocalPort(), id_caller, 0);
				}
			}
		} else {
			serverCall.startServer();
		}
	}

	public ServerCall getServerCall() {
		return serverCall;
	}

	public void setServerCall(ServerCall serverCall) {
		this.serverCall = serverCall;
	}

	public ReceiveCall getReceiveCall() {
		return receiveCall;
	}

	public void setReceiveCall(ReceiveCall receiveCall) {
		this.receiveCall = receiveCall;
	}

	public int getId_caller() {
		return id_caller;
	}

	public void setId_caller(int id_caller) {
		this.id_caller = id_caller;
	}

	public void endCall() {
		running = false;

		if (soundApp != null) {
			soundApp.stopSound();
		}

		serverCall.stopServer();
		receiveCall.stop();

		// Fermer l'interface graphique
		Platform.runLater(() -> {
			if (callStage != null) {
				callStage.close();
			}
			chatController.handleEndCall(); // Notifie le parent
		});
	}

	public void answerCall() {
		try {
			soundApp.stopSound(); 
			sendOrReceive(true, true);

			if (callController != null) {
				callController.updateButtonState(true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Sound getSoundApp() {
		return soundApp;
	}

	public void setSoundApp(Sound soundApp) {
		this.soundApp = soundApp;
	}

	public AppState getAppState() {
		return appState;
	}

	public void setAppState(AppState appState) {
		this.appState = appState;
	}

	public String getIp_come() {
		return ip_come;
	}

	public void setIp_come(String ip_come) {
		this.ip_come = ip_come;
	}

	public int getPort_come() {
		return port_come;
	}

	public void setPort_come(int port_come) {
		this.port_come = port_come;
	}

}
