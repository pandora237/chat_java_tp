package com.chat_java_tp.audio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import com.chat_java_tp.ChatApp;
import com.chat_java_tp_client.helpers.Helpers;
import com.chat_java_tp_client.sound.Sound;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AudioCallWindow {

	protected Stage callStage;
	protected static final String SERVER_IP = "127.0.0.1"; // adresse IP du serveur
	protected static final int PORT = 8082;
	protected static final int bufferSize = 4096;
	protected Sound soundApp;

	protected PrintWriter out; // Pour envoyer au serveur
	protected volatile boolean running = true; // Flag pour indiquer si le client est actif
	protected Thread audioSenderThread; // Thread pour envoyer les audios
	private TargetDataLine microphone; // Ligne pour capturer l'audio du micro
	protected final ChatApp parentWin;

	public AudioCallWindow(ChatApp parentWin) {
		this.parentWin = parentWin;
		soundApp = new Sound();

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
//			System.out.println("Appel décroché");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void endCall() {
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

	public void connectToServer(boolean is_receive) {
		try {
			Socket socket = new Socket(SERVER_IP, PORT);
			if (is_receive) {

				// Thread pour envoyer les audios capturés
				new Thread(() -> {
					handleReceiveData(socket);
				}).start();
			} else {
				out = new PrintWriter(socket.getOutputStream(), true);
				// Thread pour envoyer les audios capturés
				audioSenderThread = new Thread(this::sendAudioToServer);
				audioSenderThread.start();
			}

		} catch (IOException e) {
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

	protected void handleReceiveData(Socket sender) {
		try {
			InputStream in = sender.getInputStream();
			byte[] dataBuffer = new byte[bufferSize]; // Taille réduite pour une lecture plus fréquente
			StringBuilder buffer = new StringBuilder();

			int bytesRead;
			while ((bytesRead = in.read(dataBuffer)) != -1) {
				String receivedFragment = new String(dataBuffer, 0, bytesRead);

				buffer.append(receivedFragment);

				// Traiter les messages complets
				while (buffer.indexOf(Helpers.SeparatorAudio[0]) != -1
						&& buffer.indexOf(Helpers.SeparatorAudio[1]) != -1) {
					int start = buffer.indexOf(Helpers.SeparatorAudio[0]) + Helpers.SeparatorAudio[0].length();
					int end = buffer.indexOf(Helpers.SeparatorAudio[1]);
					String completeMessage = buffer.substring(start, end);
					buffer.delete(0, end + Helpers.SeparatorAudio[1].length());

					// Décoder et vérifier les données
					String[] parts = completeMessage.split("\\|");
					if (parts.length == 2) {
						String encodedAudio = parts[0];
						String receivedChecksum = parts[1];

						if (Helpers.calculateChecksum(encodedAudio).equals(receivedChecksum)) {
							// Lecture immédiate des données audio
							Helpers.playAudioLocally(encodedAudio, bufferSize);
						} else {
							System.err.println("Erreur : les données reçues sont corrompues.");
						}
					} else {
						System.err.println("Erreur : format de message incorrect.");
					}
				}
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

}
