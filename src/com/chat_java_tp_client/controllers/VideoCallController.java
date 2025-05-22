package com.chat_java_tp_client.controllers;

import java.net.URL;
import javafx.util.Duration;

import javafx.animation.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import java.util.ResourceBundle;

import com.chat_java_tp_client.VideoCallWindow.VideoCallWindow;
import com.chat_java_tp_client.helpers.AppState;
import com.chat_java_tp_client.sound.Sound;

import javafx.scene.image.Image;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView; 
import javafx.scene.paint.Color;

public class VideoCallController implements Initializable {

	private Sound soundApp;
	private AppState appState;
	private VideoCallWindow mainVideoCallWindow;

	@FXML
	private Pane rootPane;
	@FXML
	private Button btnEndCall;
	@FXML
	private Button btnStartCall;
	@FXML
	private ImageView receiveViewVideo;
	@FXML
	private ImageView localViewVideo;
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
		canvas = new Canvas(700, 500); // adapte à ta taille
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
		mainVideoCallWindow.answerCall();
	}

	@FXML
	public void handlerEndCall(ActionEvent e) {
		mainVideoCallWindow.endCall();
		
	}

	 
	public void setMainCallApp(VideoCallWindow VideoCallWindow) {
		this.mainVideoCallWindow = VideoCallWindow;
		this.appState = VideoCallWindow.getAppState();
//		this.SocketManagerCallVideo = VideoCallWindow.getSocketManagerCallVideo();
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
