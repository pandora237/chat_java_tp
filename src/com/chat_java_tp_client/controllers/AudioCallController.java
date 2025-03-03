package com.chat_java_tp_client.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import com.chat_java_tp_client.AudioCallWindow.AudioCallWindow;
import com.chat_java_tp_client.helpers.AppState;
import com.chat_java_tp_client.helpers.SocketManagerCall;
import com.chat_java_tp_client.sound.Sound;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;

public class AudioCallController implements Initializable {

	private Sound soundApp;
	private SocketManagerCall socketManagerCall;
	private AppState appState;
	private AudioCallWindow mainAudioCallWindow;

	@FXML
	private Button btnEndCall;
	@FXML
	private Button btnStartCall;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
//		updateButtonState(false);
	}

	@FXML
	public void handlerStartCall(ActionEvent e) {
		mainAudioCallWindow.answerCall(); 
	}

	@FXML
	public void handlerEndCall(ActionEvent e) {
		mainAudioCallWindow.endCall(); 
	}

	public void setMainCallApp(AudioCallWindow audioCallWindow) {
		this.mainAudioCallWindow = audioCallWindow;
		this.appState = audioCallWindow.getAppState();
		this.socketManagerCall = audioCallWindow.getSocketManagerCall();
		this.soundApp = audioCallWindow.getSoundApp();
	}

	public void updateButtonState(boolean isCallActive) {
		Platform.runLater(() -> {
			btnStartCall.setDisable(isCallActive);
			btnStartCall.setOpacity(isCallActive ? 0.5 : 1.0);

			btnEndCall.setDisable(!isCallActive);
			btnEndCall.setOpacity(!isCallActive ? 0.5 : 1.0);
		});
	}
}
