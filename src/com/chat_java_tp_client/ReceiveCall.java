package com.chat_java_tp_client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import com.chat_java_tp_client.helpers.Helpers;

import javafx.application.Platform;

public class ReceiveCall {

	protected AudioFormat audioFormat;
	protected DataLine.Info infoAudio;
	protected SourceDataLine speaker;
	protected StringBuilder bufferStringBuilderAudio;
	protected static final int bufferSize = 8000;
	protected Thread receiveThread;

	protected Socket socket;
	protected InputStream in;
	protected PrintWriter out;

	public ReceiveCall() {
//		audioFormat = new AudioFormat(22050, 16, 1, true, false);
		audioFormat = new AudioFormat(8000, 16, 1, true, false);
//		audioFormat = new AudioFormat(44100.0f, 16, 1, true, true); 
		infoAudio = new DataLine.Info(TargetDataLine.class, audioFormat);
	}

	public void start(String ip, int port) {
		try {
			speaker = Helpers.initSpeaker(audioFormat);
			socket = new Socket(ip, port);
			in = new BufferedInputStream(socket.getInputStream());
			out = new PrintWriter(new BufferedOutputStream(socket.getOutputStream()), true);
			receiveThread = new Thread(() -> {
				handleReceiveData();
			});
			receiveThread.start();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handleReceiveData() {
		try {
			bufferStringBuilderAudio = new StringBuilder();
			byte[] dataBuffer = new byte[bufferSize];

			if (speaker == null) {
				System.err.println("Speaker not initialized");
				return;
			}

			int bytesRead;
			while (!Thread.currentThread().isInterrupted() && (bytesRead = in.read(dataBuffer)) != -1) {
				dataProcesing(dataBuffer, bytesRead);
			}
		} catch (IOException e) {
			System.out.println("Client d'appel déconnecté : ");
		} finally {

		}
	}

	protected void dataProcesing(byte[] dataBuffer, int bytesRead) {
		hanlerPlayAudioBuffer(bufferStringBuilderAudio, dataBuffer, bytesRead);
	}

	private void hanlerPlayAudioBuffer(StringBuilder bufferStringBuilder, byte[] dataBuffer, int bytesRead) {
		String receivedFragment = new String(dataBuffer, 0, bytesRead);
		if (receivedFragment.contains(Helpers.endCallType)) {
			Platform.runLater(() -> {
//				endCall();
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

	public void stop() {
		try {
			// Interrompt le thread de réception s'il est encore en vie
			if (receiveThread != null && receiveThread.isAlive()) {
				receiveThread.interrupt();
			}

			// Ferme le flux d'entrée
			if (in != null) {
				in.close();
			}

			// Ferme le flux de sortie
			if (out != null) {
				out.close();
			}

			// Ferme la socket
			if (socket != null && !socket.isClosed()) {
				socket.close();
			}

			// Ferme le haut-parleur
			if (speaker != null) {
				speaker.drain();
				speaker.close();
			}

			System.out.println("Réception d'appel arrêtée proprement.");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
