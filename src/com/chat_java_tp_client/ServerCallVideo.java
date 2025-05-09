package com.chat_java_tp_client;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import com.chat_java_tp_client.helpers.Helpers;
import com.chat_java_tp_client.controllers.VideoCallController;

import javafx.scene.image.Image;

public class ServerCallVideo {

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // Charge la lib OpenCV
	}

	Thread serverThread_video;
	private ServerSocket serverSocket_video;
	private int PORT_VIDEO;
	private final AtomicBoolean running_video = new AtomicBoolean(true);
	public Socket clientSocket;
	public VideoCallController callController;

	public void startServer(VideoCallController callController) {
		try {
			serverSocket_video = new ServerSocket(0); // Choisir automatiquement un port libre
			PORT_VIDEO = serverSocket_video.getLocalPort();
			System.out.println("Video server started on port: " + PORT_VIDEO);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		this.callController = callController;

		serverThread_video = new Thread(() -> {
			while (running_video.get()) {
				try {
					clientSocket = serverSocket_video.accept();
					new Thread(() -> {
						try {
							handleClient(clientSocket);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}).start();
				} catch (IOException e) {
					if (running_video.get()) {
						e.printStackTrace();
					} else {
						System.out.println("Serveur vidéo arrêté.");
					}
				}
			}
		});
		serverThread_video.start();
	}

	private void handleClient(Socket clientSocket) throws IOException {
		System.out.println("Client vidéo connecté : " + clientSocket.getInetAddress());

		VideoCapture camera = new VideoCapture(0); // Webcam par défaut
		OutputStream outputStream = null;

		if (!camera.isOpened()) {
			System.err.println("Impossible d’ouvrir la webcam");
			return;
		}

		try {
			Mat frame = new Mat();
			outputStream = new BufferedOutputStream(clientSocket.getOutputStream());
			PrintWriter out_send = new PrintWriter(outputStream, true);

			System.out.println("Envoi vidéo en cours... : ");

			while (running_video.get() && clientSocket.isConnected() && !Thread.currentThread().isInterrupted()) {
				camera.read(frame);
				if (!frame.empty()) {
					Image img = Helpers.matToImage(frame);
					callController.updateVideo(false, img);

					String message = Helpers.encodedData(matToByteArray(frame), false);
					System.out.println("messagemessage :: " + clientSocket + message);
					out_send.println(message);
				}
				Thread.sleep(50); // ~20 FPS
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			camera.release();
			if (outputStream != null)
				outputStream.close();
			clientSocket.close();
			System.out.println("Fin de l’envoi vidéo.");
		}
	}

	private byte[] matToByteArray(Mat frame) {
		MatOfByte buffer = new MatOfByte();
		Imgcodecs.imencode(".jpg", frame, buffer);
		return buffer.toArray();
	}

	public ServerSocket getServerSocket_video() {
		return serverSocket_video;
	}

	public void setServerSocket_video(ServerSocket serverSocket_video) {
		this.serverSocket_video = serverSocket_video;
	}

	public int getPORT_VIDEO() {
		return PORT_VIDEO;
	}

	public void stopServer() {
		running_video.set(false);

		try {
			if (clientSocket != null && !clientSocket.isClosed()) {
				clientSocket.close();
				System.out.println("Socket client vidéo fermé.");
			}

			if (serverSocket_video != null && !serverSocket_video.isClosed()) {
				serverSocket_video.close();
				System.out.println("Socket serveur vidéo fermé.");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (serverThread_video != null && serverThread_video.isAlive()) {
			serverThread_video.interrupt();
			System.out.println("Thread serveur vidéo interrompu.");
		}
	}

}
