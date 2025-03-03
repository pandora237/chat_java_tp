package com.chat_java_tp_client.audio;

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

import com.chat_java_tp_client.ChatApp;
import com.chat_java_tp_client.helpers.Helpers;
import com.chat_java_tp_client.sound.Sound;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AudioCallWindow {

	protected Stage callStage;
	protected static final String SERVER_IP = "127.0.0.1"; // adresse IP du serveur
	protected static final int PORT = 8082;
	protected static final int bufferSize = 2048;
	protected Sound soundApp;

	protected PrintWriter out; // Pour envoyer au serveur
	protected volatile boolean running = true; // Flag pour indiquer si le client est actif
	protected Thread audioSenderThread; // Thread pour envoyer les audios
	protected Thread audioReceiverThread; // Thread pour recevoir les audios
	private TargetDataLine microphone; // Ligne pour capturer l'audio du micro
	protected final ChatApp parentWin;

	protected AudioFormat audioFormat;
	protected DataLine.Info infoAudio;
	protected SourceDataLine speaker;

	public AudioCallWindow(ChatApp parentWin) {
		this.parentWin = parentWin;
		soundApp = new Sound();
		audioFormat = new AudioFormat(22050, 16, 1, true, false);
		infoAudio = new DataLine.Info(TargetDataLine.class, audioFormat);

	}

	public void startCallWindow(Boolean auto) {
		callStage = new Stage();
		callStage.setTitle("Appel Audio " + (auto ? "sortant" : "entrant"));

		Button answerButton = new Button("Décrocher");

		Button endButton = new Button("Terminer");
		endButton.setOnAction(e -> endCall());

		VBox layout = new VBox(10, answerButton, endButton);
		layout.setPadding(new Insets(10));

		Scene scene = new Scene(layout, 200, 150);
		callStage.setScene(scene);
		callStage.setOnCloseRequest(event -> {
			endCall();
			event.consume();
		});
		callStage.show();

		if (auto) {
			answerButton.setDisable(true);
			// Connexion au serveur
			connectToServer(false);
		} else {
			soundApp.playSound(Sound.CALL_AUDIO, true);
			answerButton.setOnAction(e -> answerCall());
		}
	}

	protected void answerCall() {
		try {
			soundApp.stopSound();
			connectToServer(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void endCall() {
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
		if (out != null) {
			signalEndCall();
		}
	}

	public void connectToServer(boolean is_receive) {
		try {
			Socket socket = new Socket(SERVER_IP, PORT);
			speaker = Helpers.initSpeaker(audioFormat);
			if (is_receive) {
				// Thread pour recevoir les audios capturés
				audioReceiverThread = new Thread(() -> {
					handleReceiveData(socket);
				});
				audioReceiverThread.start();
			} else {
				out = new PrintWriter(socket.getOutputStream(), true);
				// Thread pour envoyer les audios capturés
				audioSenderThread = new Thread(this::sendAudioToServer);
				audioSenderThread.start();
			}

		} catch (IOException | LineUnavailableException e) {
			e.printStackTrace();
		}
	}

	protected void sendAudioToServer() {
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
						// Encodez et envoyez immédiatement
						String message = Helpers.encodedData(buffer, true);
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

	protected void handleReceiveData(Socket sender) {
		try {
			InputStream in = sender.getInputStream();
			byte[] dataBuffer = new byte[bufferSize]; // Taille réduite pour une lecture plus fréquente
			StringBuilder buffer = new StringBuilder();

			if (speaker == null) {
				System.err.println("Speaker not init");
				return;
			}

			int bytesRead;
			while ((bytesRead = in.read(dataBuffer)) != -1) {
				hanlerPlayAudioBuffer(buffer, dataBuffer, bytesRead);
			}
		} catch (IOException e) {
			System.out.println("Client audio déconnecté : " + sender);
		} finally {
			try {
				sender.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	protected void hanlerPlayAudioBuffer(StringBuilder buffer, byte[] dataBuffer, int bytesRead) {
		String receivedFragment = new String(dataBuffer, 0, bytesRead);
		if (receivedFragment.contains(Helpers.endCallType)) {
			Platform.runLater(() -> {
				endCall();
			});
			return;
		}

		buffer.append(receivedFragment);

		// Traiter les messages complets
		while (buffer.indexOf(Helpers.SeparatorAudio[0]) != -1 && buffer.indexOf(Helpers.SeparatorAudio[1]) != -1) {
			int start = buffer.indexOf(Helpers.SeparatorAudio[0]) + Helpers.SeparatorAudio[0].length();
			int end = buffer.indexOf(Helpers.SeparatorAudio[1]);
			String completeMessage = buffer.substring(start, end);
			buffer.delete(0, end + Helpers.SeparatorAudio[1].length());

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

	private void signalEndCall() {
		if (out != null) {
			JSONObject signal = new JSONObject();
			signal.put("action", Helpers.endCallType);
			signal.put("content", "Appel terminé ::");
			out.println(signal.toString());
			out.flush();
			out = null;
		}
	}
}
