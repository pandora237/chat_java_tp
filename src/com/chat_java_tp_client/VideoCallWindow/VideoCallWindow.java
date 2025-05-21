package com.chat_java_tp_client.VideoCallWindow;

import java.io.IOException;

import com.chat_java_tp_client.ReceiveCall;
import com.chat_java_tp_client.ReceiveCallVideo;
import com.chat_java_tp_client.ServerCall;
import com.chat_java_tp_client.ServerCallVideo;
import com.chat_java_tp_client.AudioCallWindow.AudioCallWindow;
import com.chat_java_tp_client.controllers.ChatController;
import com.chat_java_tp_client.controllers.VideoCallController;
import com.chat_java_tp_client.helpers.Helpers;
import com.chat_java_tp_client.sound.Sound;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class VideoCallWindow extends AudioCallWindow {

	protected int port_come_video;
	protected int id_caller_video;

	protected ServerCallVideo serverCallVideo = new ServerCallVideo();
	protected ReceiveCallVideo receiveCallVideo = new ReceiveCallVideo();
	protected VideoCallController callControllerVideo;

	public VideoCallWindow(ChatController parentWin) {
		super(parentWin);
	}

	@Override
	public void startCallWindow(Boolean is_receive) {
		// Charger la bibliothèque OpenCV
//		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		try {
			callStage = new Stage();
			callStage.setTitle("Appel Video " + (is_receive ? "entrant" : " sortant "));

			FXMLLoader loader = new FXMLLoader(
					getClass().getResource(Helpers.getResourcesPath() + "fxml/call/videoCall.fxml"));

			Parent callView = loader.load();
			callControllerVideo = loader.getController();
			callControllerVideo.setMainCallApp(this);
			receiveCallVideo.setCallController(callControllerVideo);
			// Ajouter la vue dans le conteneur principal
			root = new StackPane();
			root.getChildren().add(callView);

			// Configurer et afficher la scène
			Scene scene = new Scene(root, 700, 500);
			callStage.setScene(scene);
			callStage.setOnCloseRequest(event -> {
				endCall();
				event.consume();
			});
			callStage.show();

			if (is_receive) {
				soundApp.playSound(Sound.CALL_VIDEO, true);
				callControllerVideo.updateButtonState(false);
			} else {
				sendOrReceive(is_receive, false);
				callControllerVideo.updateButtonState(true);
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

	@Override
	public void sendOrReceive(boolean is_receive, boolean send_signal_audio) {
		super.sendOrReceive(is_receive, send_signal_audio);
		if (is_receive) {
			if (ip_come != null) {
				receiveCallVideo.start(ip_come, port_come_video);
				// demarrage du server d'envoie
				serverCallVideo.startServer(callControllerVideo);
				chatController.signalRespCall(Helpers.videoTypeReceiver, Helpers.getLocalIpAddress(),
						serverCall.getServerSocket_audio().getLocalPort(), id_caller,
						serverCallVideo.getServerSocket_video().getLocalPort());
			}
		} else {
			serverCallVideo.startServer(callControllerVideo);
		}
	}

	@Override
	public void answerCall() {
		soundApp.stopSound();
		callControllerVideo.updateButtonState(true);
		sendOrReceive(true, false); 
	}

	@Override
	public void endCall() {
		super.endCall();
		serverCallVideo.stopServer();
		receiveCallVideo.stop();
	}

	private void safeInterrupt(Thread thread) {
		if (thread != null && thread.isAlive()) {
			thread.interrupt();
			try {
				thread.join();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				System.err.println("Erreur lors de l'arrêt du thread : " + e.getMessage());
			}
		}
	}

	public int getPort_come_video() {
		return port_come_video;
	}

	public void setPort_come_video(int port_come_video) {
		this.port_come_video = port_come_video;
	}

	public ServerCallVideo getServerCallVideo() {
		return serverCallVideo;
	}

	public void setServerCallVideo(ServerCallVideo serverCallVideo) {
		this.serverCallVideo = serverCallVideo;
	}

	public ReceiveCallVideo getReceiveCallVideo() {
		return receiveCallVideo;
	}

	public VideoCallController getCallControllerVideo() {
		return callControllerVideo;
	}

	public void setCallControllerVideo(VideoCallController callControllerVideo) {
		this.callControllerVideo = callControllerVideo;
	}

	public void setReceiveCallVideo(ReceiveCallVideo receiveCallVideo) {
		this.receiveCallVideo = receiveCallVideo;
	}

}
