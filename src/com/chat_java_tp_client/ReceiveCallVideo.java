package com.chat_java_tp_client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Base64;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import com.chat_java_tp_client.controllers.VideoCallController;
import com.chat_java_tp_client.helpers.Helpers;

import javafx.scene.image.Image;

public class ReceiveCallVideo {

	protected static final int bufferSize = 8000;

	protected Thread receiveThread;
	protected Socket socketReceive;
	protected InputStream in;
	protected PrintWriter out;

	protected StringBuilder bufferStringBuilderVideo;
	public VideoCallController callController;

	public ReceiveCallVideo() {
	}

	public void start(String ip_video, int port_video) {
		try {

			socketReceive = new Socket(ip_video, port_video);
			in = new BufferedInputStream(socketReceive.getInputStream());
			out = new PrintWriter(new BufferedOutputStream(socketReceive.getOutputStream()), true); 
			receiveThread = new Thread(() -> {
				handleReceiveData();
			});
			receiveThread.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handleReceiveData() {
		try {
			bufferStringBuilderVideo = new StringBuilder();
			byte[] dataBuffer = new byte[bufferSize];

			int bytesRead;
			while (!Thread.currentThread().isInterrupted() && (bytesRead = in.read(dataBuffer)) != -1) {
				String received = new String(dataBuffer, 0, bytesRead);
				bufferStringBuilderVideo.append(received);
				hanlerPlayVidioBuffer(bufferStringBuilderVideo);
			}

		} catch (IOException e) {
			System.out.println("Client d'appel déconnecté : ");
		} finally {

		}
	}

	public VideoCallController getCallController() {
		return callController;
	}

	public void setCallController(VideoCallController callController) {
		this.callController = callController;
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

	public void stop() {
		try {
			// Interrompt le thread de réception s'il est encore actif
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
			if (socketReceive != null && !socketReceive.isClosed()) {
				socketReceive.close();
			}

			System.out.println("Réception vidéo arrêtée proprement.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
