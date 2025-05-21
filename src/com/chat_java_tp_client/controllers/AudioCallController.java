package com.chat_java_tp_client.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import com.chat_java_tp_client.AudioCallWindow.AudioCallWindow;
import com.chat_java_tp_client.helpers.AppState;
import com.chat_java_tp_client.sound.Sound;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import javafx.animation.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class AudioCallController implements Initializable {

	private Sound soundApp;
	private AppState appState;
	private AudioCallWindow mainAudioCallWindow;

	@FXML
	private Pane rootPane;

	@FXML
	private Button btnEndCall;
	@FXML
	private Button btnStartCall;


	private Canvas canvas;
	private void drawWaves(GraphicsContext gc, double t) {
		gc.setFill(Color.rgb(0, 0, 50, 0.1)); // fond léger
		gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

		gc.setStroke(Color.CYAN);
		gc.setLineWidth(2);

		double width = canvas.getWidth();
		double height = canvas.getHeight();
		double centerY = height / 2;

		int numberOfWaves = 3;

		for (int w = 0; w < numberOfWaves; w++) {
			double phase = t + w * 0.5;
			double amplitude = 30 + 10 * Math.sin(t + w); // hauteur de l'onde
			double frequency = 0.02 + w * 0.005;

			gc.beginPath();
			for (int x = 0; x < width; x++) {
				double y = centerY + amplitude * Math.sin(frequency * x + phase);
				if (x == 0) {
					gc.moveTo(x, y);
				} else {
					gc.lineTo(x, y);
				}
			}
			gc.stroke();
		}
	}


	@Override
	public void initialize(URL location, ResourceBundle resources) {
//		updateButtonState(false);
 
		canvas = new Canvas(500, 400); // adapte à ta taille
		GraphicsContext gc = canvas.getGraphicsContext2D();

		rootPane.getChildren().add(0, canvas); // Ajouter derrière la VBox

		new AnimationTimer() {
			double time = 0;

			@Override
			public void handle(long now) {
				drawWaves(gc, time);
				time += 0.05;
			}
		}.start();
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
