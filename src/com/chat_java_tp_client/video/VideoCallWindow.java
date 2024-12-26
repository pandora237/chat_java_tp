
package com.chat_java_tp_client.video;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.videoio.VideoCapture;

import com.chat_java_tp.ChatApp;
import com.chat_java_tp.audio.AudioCallWindow;
import com.chat_java_tp_client.helpers.Helpers;
import com.chat_java_tp_client.sound.Sound;

import org.opencv.imgproc.Imgproc;
import org.opencv.core.Size;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Base64;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

public class VideoCallWindow {

	private VideoCapture videoCapture;
	private volatile boolean videoRunning = false;
	private Thread videoThread;
	private ImageView localVideoView;
	private ImageView remoteVideoView;
	protected Sound soundApp;

	protected Stage callStage;
	protected static final String SERVER_IP = "127.0.0.1"; // adresse IP du serveur
	protected static final int PORT = 8082;
	protected static final int bufferSize = 4096;

	protected PrintWriter out; // Pour envoyer au serveur
	protected volatile boolean running = true; // Flag pour indiquer si le client est actif
	protected Thread audioSenderThread; // Thread pour envoyer les audios
	private TargetDataLine microphone; // Ligne pour capturer l'audio du micro
	protected final ChatApp parentWin;

	public VideoCallWindow(ChatApp parentWin) {
		this.parentWin = parentWin;
		soundApp = new Sound();
	}

	public void startCallWindow(Boolean auto) {
		// Charger la bibliothèque OpenCV
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		// Créer une nouvelle fenêtre pour l'appel vidéo
		callStage = new Stage();
		callStage.setTitle("Appel Vidéo " + (auto ? "sortant" : "entrant"));

		// Configuration des vues vidéo
		localVideoView = new ImageView();
		remoteVideoView = new ImageView();

		// Charger une image par défaut pour la vue distante
		Image videoCallImage = new Image("file:img/videoCall.png");
		remoteVideoView.setImage(videoCallImage);
		remoteVideoView.setFitWidth(400); // Ajuster la largeur
		remoteVideoView.setPreserveRatio(true);

		// Charger une image par défaut pour la vue locale
		localVideoView.setImage(videoCallImage);
		localVideoView.setFitWidth(400);
		localVideoView.setPreserveRatio(true);

		// Boutons de contrôle
		Button startVideoButton = new Button("Démarrer Vidéo");
		startVideoButton.setOnAction(e -> answerCall());

		Button endCallButton = new Button("Terminer Appel");
		endCallButton.setOnAction(e -> endCall());

		HBox videoLayout = new HBox(20, localVideoView, remoteVideoView);
		videoLayout.setAlignment(Pos.CENTER);

		HBox controls = new HBox(10, startVideoButton, endCallButton);
		controls.setAlignment(Pos.CENTER);

		VBox mainLayout = new VBox(20, videoLayout, controls);
		mainLayout.setAlignment(Pos.CENTER);

		// Configurer et afficher la scène
		Scene scene = new Scene(mainLayout, 900, 800);
		callStage.setScene(scene);
		callStage.setOnCloseRequest(event -> {
			endCall();
			event.consume();
		});
		callStage.show();

		if (auto) {
			startVideoButton.setDisable(true); // Désactiver le bouton pour un appel sortant
			connectToServer(false); // Connexion au serveur
		} else {
			soundApp.playSound(Sound.CALL_AUDIO, true); // Jouer un son pour un appel entrant
			startVideoButton.setOnAction(e -> {
				answerCall(); // Démarrer la capture vidéo
				soundApp.stopSound(); // Arrêter le son de l'appel
			});
		}
	}

	protected void answerCall() {
		try {
			soundApp.stopSound();
			connectToServer(true);
//			System.out.println("Appel décroché");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void sendAudioToServer() {
		try {
			AudioFormat format = new AudioFormat(22050, 16, 1, true, false);
			DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

			if (!AudioSystem.isLineSupported(info)) {
				System.err.println("Format audio non supporté : " + format);
				return;
			}

			try (TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info)) {
				microphone.open(format);
				microphone.start();

				byte[] buffer = new byte[bufferSize];
				ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

				while (running) {
					int bytesRead = microphone.read(buffer, 0, buffer.length);
					if (bytesRead > 0) {
						byteStream.write(buffer, 0, bytesRead);

						// Encodez et envoyez immédiatement
						String message = Helpers.encodedData(byteStream.toByteArray(), true);
						synchronized (out) {
							out.println(message);
						}

						byteStream.reset(); // Réinitialisation pour le prochain paquet
					}
				}
			}
		} catch (LineUnavailableException e) {
			System.err.println("Ligne audio non disponible : " + e.getMessage());
		}
	}

	private void sendVideoToServer(Mat frame) {
		// Convertir la trame en tableau d'octets, encoder en Base64 et envoyer
		MatOfByte buffer = new MatOfByte();
		Imgcodecs.imencode(".bmp", frame, buffer);

		String message = Helpers.encodedData(buffer.toArray(), true);
		synchronized (out) {
			out.println(message);
		}
	}

	private void processLocalVideo() {
		Mat frame = new Mat();
		while (videoRunning) {
			if (videoCapture.read(frame)) {
				// Convertir Mat en Image et l'afficher dans localVideoView
				Image img = Helpers.matToImage(frame);
				Platform.runLater(() -> localVideoView.setImage(img));
				// Envoyer le flux vidéo au serveur
				sendVideoToServer(frame);
			}
		}
		frame.release();
	}

	public void connectToServer(boolean is_receive) {
		try {
			Socket socket = new Socket(SERVER_IP, PORT);

			if (is_receive) {
				// Thread pour gérer la réception de données (audio et vidéo)
				new Thread(() -> {
					handleReceiveData(socket);
				}).start();
			} else {
				// Initialisation du flux de sortie pour l'envoi de données
				out = new PrintWriter(socket.getOutputStream(), true);

				// Thread pour envoyer les audios capturés
				audioSenderThread = new Thread(() -> sendAudioToServer());
				audioSenderThread.start();

				// Initialisation de la capture vidéo
				videoCapture = new VideoCapture(0); // 0 pour la webcam par défaut
				if (!videoCapture.isOpened()) {
					System.err.println("Erreur : Impossible d'accéder à la caméra.");
					return;
				}

				// Thread pour capturer et envoyer les vidéos
				videoRunning = true;
				videoThread = new Thread(() -> {
					try {
						processLocalVideo();
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
		} catch (IOException e) {
			System.err.println("Erreur : Impossible de se connecter au serveur - " + e.getMessage());
		}
	}

	protected void handleReceiveData(Socket sender) {
		try {
			InputStream in = sender.getInputStream();
			byte[] dataBuffer = new byte[bufferSize]; // Taille réduite pour une lecture plus fréquente
			StringBuilder buffer = new StringBuilder();

			int bytesRead;
			while ((bytesRead = in.read(dataBuffer)) != -1) {
				String receivedFragment = new String(dataBuffer, 0, bytesRead);
				buffer.append(receivedFragment);

				// Traiter les messages complets pour audio et vidéo
				while (buffer.indexOf(Helpers.SeparatorAudio[0]) != -1
						&& buffer.indexOf(Helpers.SeparatorAudio[1]) != -1) {
					int start = buffer.indexOf(Helpers.SeparatorAudio[0]) + Helpers.SeparatorAudio[0].length();
					int end = buffer.indexOf(Helpers.SeparatorAudio[1]);
					String completeMessage = buffer.substring(start, end);
					buffer.delete(0, end + Helpers.SeparatorAudio[1].length());

					// Décoder et vérifier les données audio
					String[] parts = completeMessage.split("\\|");
					if (parts.length == 2) {
						String encodedAudio = parts[0];
						String receivedChecksum = parts[1];

						if (Helpers.calculateChecksum(encodedAudio).equals(receivedChecksum)) {
							// Lecture immédiate des données audio
							Helpers.playAudioLocally(encodedAudio, bufferSize);
						} else {
							System.err.println("Erreur : les données audio reçues sont corrompues.");
						}
					} else {
						System.err.println("Erreur : format de message audio incorrect.");
					}
				}

				while (buffer.indexOf(Helpers.SeparatorVideo[0]) != -1
						&& buffer.indexOf(Helpers.SeparatorVideo[1]) != -1) {
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
								displayVideoReceiveFrame(videoFrame);
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
		} catch (IOException e) {
			System.out.println("Client déconnecté : " + sender);
		} finally {
			try {
				sender.close();
			} catch (IOException e) {
				e.printStackTrace();
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

	private void displayVideoReceiveFrame(Mat frame) {
		Image image = Helpers.matToImage(frame);
		// afficher l'image dans une interface graphique
		Platform.runLater(() -> remoteVideoView.setImage(image));
	}

	protected void endCall() {
		parentWin.handleEndCall();
		running = false; // Arrête le thread d'écoute et d'envoi
		if (microphone != null) {
			microphone.close();
		}
		if (audioSenderThread != null && audioSenderThread.isAlive()) {
			audioSenderThread.interrupt();
		}
		soundApp.stopSound();
		callStage.close();
		parentWin.handleEndCall();
		System.out.println("Appel terminé");
	}

}
