package com.chat_java_tp_client;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import com.chat_java_tp_client.helpers.Helpers;

public class ServerCall {

	private ServerSocket serverSocket_audio;
	private int PORT_AUDIO;
	private final AtomicBoolean running_audio = new AtomicBoolean(true);

	protected AudioFormat audioFormat;
	protected DataLine.Info infoAudio;
	protected SourceDataLine speaker;
	protected static final int bufferSize = 8000;
	public Socket clientSocket;
	private Thread serverThread_audio;

	public ServerCall() {
//		audioFormat = new AudioFormat(22050, 16, 1, true, false);
		audioFormat = new AudioFormat(8000, 16, 1, true, false);
//		audioFormat = new AudioFormat(44100.0f, 16, 1, true, true); 
		infoAudio = new DataLine.Info(TargetDataLine.class, audioFormat);
	}

	public void startServer() {
		try {
			serverSocket_audio = new ServerSocket(0); // Port libre choisi automatiquement
			PORT_AUDIO = serverSocket_audio.getLocalPort();
			System.out.println("Audio server started on port: " + PORT_AUDIO);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		serverThread_audio = new Thread(() -> {
			while (running_audio.get()) {
				try {
					clientSocket = serverSocket_audio.accept(); // Accept incoming connection
					new Thread(() -> {
						try {
							handleClient(clientSocket);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}).start();
				} catch (IOException e) {
					if (running_audio.get()) {
						e.printStackTrace();
					} else {
						System.out.println("Serveur Audio arrêté.");
					}
				}
			}
		});

		serverThread_audio.start();
	}

	private void handleClient(Socket clientSocket) throws IOException {
		System.out.println(
				"Client audio connecté : " + clientSocket.getInetAddress() + " " + clientSocket.getLocalPort());

		TargetDataLine microphone = null;
		OutputStream outputStream = null;

		try {
			if (!AudioSystem.isLineSupported(infoAudio)) {
				System.err.println("Format audio non supporté : " + audioFormat);
				return;
			}

			microphone = (TargetDataLine) AudioSystem.getLine(infoAudio);
			microphone.open(audioFormat);
			microphone.start();

			byte[] buffer = new byte[bufferSize];
			outputStream = new BufferedOutputStream(clientSocket.getOutputStream());
			PrintWriter out_send = new PrintWriter(outputStream, true);

			System.out.println(" Envoi du son en cours...");

			while (running_audio.get() && clientSocket.isConnected()) {
				int bytesRead = microphone.read(buffer, 0, buffer.length);
				System.out.println(bytesRead);
				if (bytesRead > 0) {
					String message = Helpers.encodedData(buffer, true);
					out_send.println(message);
				}
			}

		} catch (LineUnavailableException e) {
			System.err.println("Ligne audio non disponible : " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (microphone != null) {
				microphone.stop();
				microphone.close();
			}
			if (outputStream != null) {
				outputStream.close();
			}
			clientSocket.close();
			System.out.println(" Fin de l’envoi audio.");
		}
	}

	public ServerSocket getServerSocket_audio() {
		return serverSocket_audio;
	}

	public void setServerSocket_audio(ServerSocket serverSocket_audio) {
		this.serverSocket_audio = serverSocket_audio;
	}

	public void stopServer() {
		running_audio.set(false);

		try {
			if (clientSocket != null && !clientSocket.isClosed()) {
				clientSocket.close();
				System.out.println("Socket client audio fermé.");
			}

			if (serverSocket_audio != null && !serverSocket_audio.isClosed()) {
				serverSocket_audio.close();
				System.out.println("Socket serveur audio fermé.");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
