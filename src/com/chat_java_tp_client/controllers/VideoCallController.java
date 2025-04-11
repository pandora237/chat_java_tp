package com.chat_java_tp_client.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import com.chat_java_tp_client.VideoCallWindow.VideoCallWindow;
import com.chat_java_tp_client.helpers.AppState;
import com.chat_java_tp_client.helpers.SocketManagerCallVideo;
import com.chat_java_tp_client.sound.Sound;

import javafx.scene.image.Image;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;

public class VideoCallController implements Initializable {

	private Sound soundApp;
	private SocketManagerCallVideo SocketManagerCallVideo;
	private AppState appState;
	private VideoCallWindow mainVideoCallWindow;

	@FXML
	private Button btnEndCall;
	@FXML
	private Button btnStartCall;
	@FXML
	private ImageView receiveViewVideo;
	@FXML
	private ImageView localViewVideo;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
//		updateButtonState(false);
	}

	@FXML
	public void handlerStartCall(ActionEvent e) {
		mainVideoCallWindow.answerCall();
	}

	@FXML
	public void handlerEndCall(ActionEvent e) {
		mainVideoCallWindow.endCall();
	}

	public void setMainCallApp(VideoCallWindow VideoCallWindow) {
		this.mainVideoCallWindow = VideoCallWindow;
		this.appState = VideoCallWindow.getAppState();
		this.SocketManagerCallVideo = VideoCallWindow.getSocketManagerCallVideo();
		this.soundApp = VideoCallWindow.getSoundApp();
	}

	public void updateButtonState(boolean isCallActive) {
		Platform.runLater(() -> {
			btnStartCall.setDisable(isCallActive);
			btnStartCall.setOpacity(isCallActive ? 0.5 : 1.0);

			btnEndCall.setDisable(!isCallActive);
			btnEndCall.setOpacity(!isCallActive ? 0.5 : 1.0);
		});
	}

	public void updateVideo(Boolean is_receive, Image img) {
		Platform.runLater(() -> {
			if (is_receive) {
				receiveViewVideo.setImage(img);
			} else {
				localViewVideo.setImage(img);
			}
		});

	}

}
