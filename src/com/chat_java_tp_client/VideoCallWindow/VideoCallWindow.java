package com.chat_java_tp_client.VideoCallWindow;

import java.io.IOException;
import java.util.Base64;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import com.chat_java_tp_client.AudioCallWindow.AudioCallWindow;
import com.chat_java_tp_client.controllers.ChatController;
import com.chat_java_tp_client.controllers.VideoCallController;
import com.chat_java_tp_client.helpers.Helpers;
import com.chat_java_tp_client.sound.Sound;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class VideoCallWindow extends AudioCallWindow {

	private VideoCapture videoCapture;
	private volatile boolean videoRunning = false;
	private Thread videoThread;
	VideoCallController callController;

	protected Stage callStage;

	public VideoCallWindow(ChatController parentWin) {
		super(parentWin);
	}

	@Override
	public void startCallWindow(Boolean is_receive) {
		// Charger la bibliothèque OpenCV
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		try {
			callStage = new Stage();
			callStage.setTitle("Appel Audio " + (is_receive ? "entrant" : " sortant "));

			FXMLLoader loader = new FXMLLoader(
					getClass().getResource(Helpers.getResourcesPath() + "fxml/call/videoCall.fxml"));

			Parent callView = loader.load();
			callController = loader.getController();
			callController.setMainCallApp(this);

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
				callController.updateButtonState(false);
			} else {
				socketManagerCall.connect();
				sendOrReceive(is_receive);
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

	@Override
	public void answerCall() {
		super.answerCall();
	}

	@Override
	public void endCall() {
		super.endCall();
		// Arrêter et libérer la capture vidéo
		if (videoRunning) {
			videoRunning = false;
			if (videoThread != null && videoThread.isAlive()) {
				videoThread.interrupt();
			}
		}

		// Libérer les ressources de la vidéo
		if (videoCapture != null && videoCapture.isOpened()) {
			videoCapture.release();
			videoCapture = null;
		}

//		// Assurez-vous de libérer la vue vidéo locale et distante
//		Platform.runLater(() -> {
//			localVideoView.setImage(null);
//			remoteVideoView.setImage(null);
//		});

		// Fermer la fenêtre de l'appel
		if (callStage != null) {
			callStage.close();
		}
	}

	@Override
	protected void dataProcesing(byte[] dataBuffer, int bytesRead) {
		super.dataProcesing(dataBuffer, bytesRead);
		hanlerPlayVidioBuffer(bufferStringBuilderAudio);
//		hanlerPlayAudioBuffer(bufferStringBuilderAudio, dataBuffer, bytesRead);

	}

	@Override
	protected void captureAndsendDatas(Boolean is_receive) {
		super.captureAndsendDatas(is_receive);
		if (!is_receive) {
			// Initialisation de la capture vidéo
			videoCapture = new VideoCapture(0); // 0 pour la webcam par défaut
			if (!videoCapture.isOpened()) {
				System.err.println("Erreur : Impossible d'accéder à la caméra.");
				return;
			}

			// Thread pour capturer et envoyer les vidéos
			videoRunning = true;
			System.out.println("avant video");
			videoThread = new Thread(() -> {
				try {
					Mat frame = new Mat();
					while (videoRunning) {
						if (videoCapture.read(frame)) {
							// Convertir Mat en Image et l'afficher dans localVideoView
							Image img = Helpers.matToImage(frame);
							callController.updateVideo(false, img);
							// Envoyer le flux vidéo au serveur
							sendVideoToServer(frame);
						}
					}
					frame.release();
				} catch (Exception e) {
					System.err.println("Erreur lors du traitement de la vidéo : " + e.getMessage());
				} finally {
					// Assurez-vous de libérer la caméra
					if (videoCapture != null && videoCapture.isOpened()) {
						videoCapture.release();
					}
				}
			});
			videoThread.start();
		}

	}

	private void sendVideoToServer(Mat frame) {
		// Convertir la trame en tableau d'octets, encoder en Base64 et envoyer
		MatOfByte buffer = new MatOfByte();
		Imgcodecs.imencode(".bmp", frame, buffer);

		String message = Helpers.encodedData(buffer.toArray(), false);
		synchronized (socketManagerCall.getOutputStream()) {
			socketManagerCall.getOutputStream().println(message);
		}
	}

	protected void hanlerPlayVidioBuffer(StringBuilder buffer) {
		while (buffer.indexOf(Helpers.SeparatorVideo[0]) != -1 && buffer.indexOf(Helpers.SeparatorVideo[1]) != -1) {
			int start = buffer.indexOf(Helpers.SeparatorVideo[0]) + Helpers.SeparatorVideo[0].length();
			int end = buffer.indexOf(Helpers.SeparatorVideo[1]);
			String completeMessage = buffer.substring(start, end);
			buffer.delete(0, end + Helpers.SeparatorVideo[1].length());

			// Décoder et vérifier les données vidéo
			String[] parts = completeMessage.split("\\|");
			if (parts.length == 2) {
				String encodedVideo = parts[0];
				String receivedChecksum = parts[1];

				if (Helpers.calculateChecksum(encodedVideo).equals(receivedChecksum)) {
					// Convertir les données vidéo encodées en Base64 en Mat (image)
					Mat videoFrame = decodeVideo(encodedVideo);
					// Afficher l'image vidéo
					if (videoFrame != null) {
						Image img = Helpers.matToImage(videoFrame);
						callController.updateVideo(true, img);
					} else {
						System.err.println("Erreur : les données vidéo reçues sont invalides.");
					}
				} else {
					System.err.println("Erreur : les données vidéo reçues sont corrompues.");
				}
			} else {
				System.err.println("Erreur : format de message vidéo incorrect.");
			}
		}
	}

	private Mat decodeVideo(String encodedData) {
		try {
			byte[] decodedBytes = Base64.getDecoder().decode(encodedData);
			Mat frame = Imgcodecs.imdecode(new MatOfByte(decodedBytes), Imgcodecs.IMREAD_COLOR);
			return frame;
		} catch (Exception e) {
			System.err.println("Erreur lors du décodage des données vidéo : " + e.getMessage());
			return null;
		}
	}
}
