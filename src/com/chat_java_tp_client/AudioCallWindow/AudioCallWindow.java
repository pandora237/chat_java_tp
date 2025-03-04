package com.chat_java_tp_client.AudioCallWindow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import org.json.JSONObject;

import com.chat_java_tp_client.controllers.AudioCallController;
import com.chat_java_tp_client.controllers.ChatController;
import com.chat_java_tp_client.helpers.AppState;
import com.chat_java_tp_client.helpers.Helpers;
import com.chat_java_tp_client.helpers.SocketManagerCall;
import com.chat_java_tp_client.helpers.SocketManagerMessage;
import com.chat_java_tp_client.sound.Sound;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AudioCallWindow {

	protected Stage callStage;
//	protected static final String SERVER_IP = "127.0.0.1"; // adresse IP du serveur
//	protected static final int PORT = 8082;
	protected static final int bufferSize = 15048;

	protected volatile boolean running = true; // Flag pour indiquer si le client est actif
	protected Thread audioSenderThread; // Thread pour envoyer les audios
	protected Thread audioReceiverThread; // Thread pour recevoir les audios
	private TargetDataLine microphone; // Ligne pour capturer l'audio du micro
	protected final ChatController parentWin;

	protected AudioFormat audioFormat;
	protected DataLine.Info infoAudio;
	protected SourceDataLine speaker;
	private AudioCallController callController;

	protected SocketManagerCall socketManagerCall = new SocketManagerCall();

	protected Sound soundApp;
	protected StackPane root;
	private AppState appState;
	protected StringBuilder bufferStringBuilderAudio;

	public AudioCallWindow(ChatController parentWin) {
		this.parentWin = parentWin;
		soundApp = new Sound();
		audioFormat = new AudioFormat(22050, 16, 1, true, false);
		infoAudio = new DataLine.Info(TargetDataLine.class, audioFormat);
		socketManagerCall = new SocketManagerCall();
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

//		Button answerButton = new Button("Décrocher");
//
//		Button endButton = new Button("Terminer");
//		endButton.setOnAction(e -> endCall());
//
//		VBox layout = new VBox(10, answerButton, endButton);
//		layout.setPadding(new Insets(10));
//
//		Scene scene = new Scene(layout, 200, 150);
//		callStage.setScene(scene);
//		callStage.setOnCloseRequest(event -> {
//			endCall();
//			event.consume();
//		});
//		callStage.show();
//
//		if (auto) {
//			answerButton.setDisable(true);
//			// Connexion au serveur
//			sendOrReceive(false);
//		} else {
//			soundApp.playSound(Sound.CALL_AUDIO, true);
//			answerButton.setOnAction(e -> answerCall());
//		}
	}

	public void sendOrReceive(boolean is_receive) {
		try {
			speaker = Helpers.initSpeaker(audioFormat);
			if (is_receive) {
				// Thread pour recevoir les donnees capturés
				audioReceiverThread = new Thread(() -> {
					handleReceiveData();
				});
				audioReceiverThread.start();
			} else {
				captureAndsendDatas(is_receive);

			}

		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
	}

	protected void captureAndsendDatas(Boolean is_receive) {

		// Thread pour envoyer les donnees capturés
		audioSenderThread = new Thread(() -> {
			try {
				if (!AudioSystem.isLineSupported(infoAudio)) {
					System.err.println("Format audio non supporté : " + audioFormat);
					return;
				}

				try (TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(infoAudio)) {
					microphone.open(audioFormat);
					microphone.start();

					byte[] buffer = new byte[bufferSize];
					ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

					while (running) {
						int bytesRead = microphone.read(buffer, 0, buffer.length);
						if (bytesRead > 0) {
							// Encodez et envoyez
							String message = Helpers.encodedData(buffer, true);
							synchronized (socketManagerCall.getOutputStream()) {
								socketManagerCall.getOutputStream().println(message);
							}
							byteStream.reset(); // Réinitialisation pour le prochain paquet
						}
					}
				}
			} catch (LineUnavailableException e) {
				System.err.println("Ligne audio non disponible : " + e.getMessage());
			}
		});
		audioSenderThread.start();

	}

	private void handleReceiveData() {
		try {
			bufferStringBuilderAudio = new StringBuilder();
			byte[] dataBuffer = new byte[bufferSize]; // Taille réduite pour une lecture plus fréquente

			if (speaker == null) {
				System.err.println("Speaker not initialized");
				return;
			}

			int bytesRead;
			while ((bytesRead = socketManagerCall.getInputStream().read(dataBuffer)) != -1) {
				dataProcesing(dataBuffer, bytesRead);
//				hanlerPlayAudioBuffer(bufferStringBuilderAudio, dataBuffer, bytesRead);
			}
		} catch (IOException e) {
			System.out.println("Client d'appel déconnecté : " + socketManagerCall.getSocket());
		} finally {
			try {
				socketManagerCall.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	protected void dataProcesing(byte[] dataBuffer, int bytesRead) {
		hanlerPlayAudioBuffer(bufferStringBuilderAudio, dataBuffer, bytesRead);
	}

	private void hanlerPlayAudioBuffer(StringBuilder bufferStringBuilder, byte[] dataBuffer, int bytesRead) {
		String receivedFragment = new String(dataBuffer, 0, bytesRead);
		if (receivedFragment.contains(Helpers.endCallType)) {
			Platform.runLater(() -> {
				endCall();
			});
			return;
		}

		bufferStringBuilder.append(receivedFragment);

		// Traiter les messages complets
		while (bufferStringBuilder.indexOf(Helpers.SeparatorAudio[0]) != -1
				&& bufferStringBuilder.indexOf(Helpers.SeparatorAudio[1]) != -1) {
			int start = bufferStringBuilder.indexOf(Helpers.SeparatorAudio[0]) + Helpers.SeparatorAudio[0].length();
			int end = bufferStringBuilder.indexOf(Helpers.SeparatorAudio[1]);

			if (start >= end || start < 0 || end < 0) {
				System.err.println("Indices invalides : start=" + start + ", end=" + end + ", buffer.length="
						+ bufferStringBuilder.length());
				break; // Sortir de la boucle pour éviter une boucle infinie
			}

			String completeMessage = bufferStringBuilder.substring(start, end);
			bufferStringBuilder.delete(0, end + Helpers.SeparatorAudio[1].length());

			// Décoder et vérifier les données
			String[] parts = completeMessage.split("\\|");
			if (parts.length == 2) {
				String encodedAudio = parts[0];
				String receivedChecksum = parts[1];

				if (Helpers.calculateChecksum(encodedAudio).equals(receivedChecksum) && speaker != null) {
					// decodage et Lecture des données audio
					Helpers.decodeAndPlayAudioLocally(encodedAudio, bufferSize, speaker, audioFormat, infoAudio);
				} else {
					System.err.println("Erreur : les données reçues sont corrompues.");
				}
			} else {
				System.err.println("Erreur : format de message incorrect.");
			}
		}
	}

	public void endCall() {
		running = false; // Arrête le thread d'écoute et d'envoi

		// Fermeture du microphone
		if (microphone != null) {
			microphone.close();
		}

		// Fermeture du thread d'envoi audio
		if (audioSenderThread != null && audioSenderThread.isAlive()) {
			audioSenderThread.interrupt();
		}

		// Arrêt du thread de réception et fermeture du speaker
		if (soundApp != null) {
			soundApp.stopSound();
		}

		// Fermer le SourceDataLine (speaker)
		if (speaker != null && speaker.isOpen()) {
			speaker.stop();
			speaker.close();
			speaker = null;
		}

		// S'assurer que le thread de réception est arrêté
		// (vous pourriez avoir un thread de réception qui gère la lecture du audio du
		// serveur)
		if (audioReceiverThread != null && audioReceiverThread.isAlive()) {
			audioReceiverThread.interrupt(); // Arrêter le thread de réception
		}

		// Fermeture de la scène de l'appel
		Platform.runLater(() -> {
			if (callStage != null) {
				callStage.close();
			}

			// Notifier le parent (par exemple pour réinitialiser l'état de l'application)
			parentWin.handleEndCall();

		});

		// Envoi d'un message au serveur pour notifier de la fin de l'appel
		if (socketManagerCall.getOutputStream() != null) {
			signalEndCall();
		}
	}

	private void signalEndCall() {
		PrintWriter out = socketManagerCall.getOutputStream();
		if (out != null) {
			JSONObject signal = new JSONObject();
			signal.put("action", Helpers.endCallType);
			signal.put("content", "Appel terminé ::");
			out.println(signal.toString());
			out.flush();
			out = null;
		}
	}

	public void answerCall() {
		try {
			soundApp.stopSound();
			socketManagerCall.connect();
			sendOrReceive(true);
			callController.updateButtonState(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public SocketManagerCall getSocketManagerCall() {
		return socketManagerCall;
	}

	public void setSocketManagerCall(SocketManagerCall socketManagerCall) {
		this.socketManagerCall = socketManagerCall;
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

}
