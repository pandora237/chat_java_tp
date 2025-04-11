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
import com.chat_java_tp_client.helpers.SocketManagerCallVideo;
import com.chat_java_tp_client.sound.Sound;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class VideoCallWindow extends AudioCallWindow {

	protected boolean receiveTransmit = false; // utiliser pour activer l'envoie et reception de receveur d'appel

	protected Stage callStage;
	protected SocketManagerCallVideo socketManagerCallVideo = new SocketManagerCallVideo();
	protected VideoCallController callController;
	private volatile boolean videoRunning = false;
	protected StringBuilder bufferStringBuilderVideo;

	private VideoCapture videoCapture;
	protected Thread videoSenderThreadSend; // Thread pour envoyer les videos
	protected Thread videoReceiverThreadSend; // Thread pour recevoir les videos

	/*
	 * Receive
	 */
	private VideoCapture videoCaptureReceiveReceiver;
	protected Thread videoSenderThreadReceive; // Thread pour envoyer les videos
	protected Thread videoReceiverThreadReceive; // Thread pour recevoir les videos

	public VideoCallWindow(ChatController parentWin) {
		super(parentWin);
	}

	public SocketManagerCallVideo getSocketManagerCallVideo() {
		return socketManagerCallVideo;
	}

	public void setSocketManagerCallVideo(SocketManagerCallVideo socketManagerCallVideo) {
		this.socketManagerCallVideo = socketManagerCallVideo;
	}

	@Override
	public void startCallWindow(Boolean is_receive) {
		// Charger la bibliothèque OpenCV
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		try {
			callStage = new Stage();
			callStage.setTitle("Appel Video " + (is_receive ? "entrant" : " sortant "));

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
				System.err.println("callController________ " + callController);
			} else {
				socketManagerCallVideo.connect();
				socketManagerCall.connect();
				sendOrReceive(is_receive);
				sendOrReceiveVideo(is_receive);
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

	public void sendOrReceiveVideo(boolean is_receive) {
		if (is_receive) {
			if (videoReceiverThreadSend == null || !videoReceiverThreadSend.isAlive()) {
				videoReceiverThreadSend = new Thread(() -> handleReceiveDataVideo(is_receive));
				videoReceiverThreadSend.start();
			}
			if (receiveTransmit) {
				if (videoSenderThreadReceive == null || !videoSenderThreadReceive.isAlive()) {
					videoSenderThreadReceive = new Thread(() -> captureAndsendDatasVideo(is_receive));
					videoSenderThreadReceive.start();
				}
			}
		} else {
			if (videoSenderThreadSend == null || !videoSenderThreadSend.isAlive()) {
				videoSenderThreadSend = new Thread(() -> captureAndsendDatasVideo(is_receive));
				videoSenderThreadSend.start();
			}
			if (receiveTransmit) {
				if (videoReceiverThreadReceive == null || !videoReceiverThreadReceive.isAlive()) {
					videoReceiverThreadReceive = new Thread(() -> handleReceiveDataVideo(is_receive));
					videoReceiverThreadReceive.start();
				}
			}
		}
	}

	private void handleReceiveDataVideo(boolean is_receive) {
		try {
			bufferStringBuilderVideo = new StringBuilder();
			byte[] dataBuffer = new byte[bufferSize]; // Taille réduite pour une lecture plus fréquente

			if (speaker == null) {
				System.err.println("Speaker not initialized");
				return;
			}

			int bytesRead;
			if (is_receive) {
				while ((bytesRead = socketManagerCall.getInputStreamSend().read(dataBuffer)) != -1) {
					hanlerPlayVidioBuffer(bufferStringBuilderVideo, is_receive);
				}
			} else {
				while ((bytesRead = socketManagerCall.getInputStreamReceive().read(dataBuffer)) != -1) {
					hanlerPlayVidioBuffer(bufferStringBuilderVideo, is_receive);
				}
			}

		} catch (IOException e) {
//			System.out.println("Client d'appel déconnecté : " + socketManagerCall.getSocket());
			System.out.println("Client d'appel déconnecté : ");
		} finally {
			socketManagerCall.close();
		}
	}

	@Override
	public void answerCall() {
		callController.updateButtonState(true);
		super.answerCall();
		sendOrReceiveVideo(true);
	}

	@Override
	public void endCall() {
		super.endCall();
		videoRunning = false;

		safeInterrupt(videoSenderThreadSend);
		safeInterrupt(videoReceiverThreadSend);
		safeInterrupt(videoSenderThreadReceive);
		safeInterrupt(videoReceiverThreadReceive);

		if (videoCapture != null && videoCapture.isOpened()) {
			videoCapture.release();
			videoCapture = null;
		}

		if (callStage != null) {
			callStage.close();
		}
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

	protected void captureAndsendDatasVideo(Boolean is_receive) {

		// Initialisation de la capture vidéo
		videoCapture = new VideoCapture(0); // 0 pour la webcam par défaut
		if (!videoCapture.isOpened()) {
			System.err.println("Erreur : Impossible d'accéder à la caméra.");
			return;
		}

		// Thread pour capturer et envoyer les vidéos
		videoRunning = true;
		try {
			Mat frame = new Mat();
			while (videoRunning) {
				if (videoCapture.read(frame)) {
					// Convertir Mat en Image et l'afficher dans localVideoView
					Image img = Helpers.matToImage(frame);
					callController.updateVideo(is_receive, img);
					// Envoyer le flux vidéo au serveur
					sendVideoToServer(frame, is_receive);
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
	}

	private void sendVideoToServer(Mat frame, Boolean is_receive) {
		// Convertir la trame en tableau d'octets, encoder en Base64 et envoyer
		MatOfByte buffer = new MatOfByte();
		Imgcodecs.imencode(".bmp", frame, buffer);

		String message = Helpers.encodedData(buffer.toArray(), false);
		if (is_receive) {
			synchronized (socketManagerCallVideo.getOutputStreamReceive()) {
				socketManagerCallVideo.getOutputStreamReceive().println(message);
			}
		} else {
			synchronized (socketManagerCallVideo.getOutputStreamSend()) {
				socketManagerCallVideo.getOutputStreamSend().println(message);
			}
		}

	}

	protected void hanlerPlayVidioBuffer(StringBuilder buffer, boolean is_receive) {
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
						callController.updateVideo(is_receive, img);
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
