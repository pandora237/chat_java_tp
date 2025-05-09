package com.chat_java_tp_client.helpers;

import java.awt.Desktop;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class Helpers {
	// commun server
	public static final String sendFile = "send_file";
	public static final String sendSimpleMess = "send_file";
	public static final String askFile = "ask_file";
	public static final String audioType = "audio_call";
	public static final String audioTypeResp = "audio_call_resp";
	public static final String videoType = "video_call";
	public static final String emoji = "emoji";
	public static final String audioTypeReceiver = "audio_call_receiver";
	public static final String videoTypeReceiver = "video_call_receiver";

	public static final String endCallType = "end_call";
	public static final String login = "login";
	public static final String logout = "logout";
	public static final String otherUserLogged = "other_user_logged";
	public static final String deliveredPortCall = "deliveredPortCall";

	public static final String responseSendMessage = "response_send_message";
	public static final String getMessUserSendReceive = "get_mess_user_send_receive";

	// end commun

	public static final String FILE_DOWNLOAD = "downloads/";
	public static final String imageDirectoryPathEmoji = "src/" + Helpers.getResourcesPath() + "emoji";

	public static final String[] SeparatorVideo = { "START_VIDEO", "END_VIDEO " };
	public static final String[] SeparatorAudio = { "START_AUDIO", "END_AUDIO " };

	public static void openFileFolder(String reltifPath) {
		try {
			File folder = new File(reltifPath).getAbsoluteFile();

			if (!folder.exists()) {
				boolean created = folder.mkdirs();
				if (!created) {
					System.err.println("Échec de la création du dossier : " + folder.getAbsolutePath());
					return;
				}
			}

			Desktop.getDesktop().open(folder);

		} catch (IOException e) {
			System.err.println("Erreur lors de l'ouverture du dossier.");
			e.printStackTrace();
		}
	}

	public static Image matToImage(Mat frame) {
		try {
			if (frame.empty()) {
				System.err.println("Erreur : Mat est vide");
				return null;
			}

			// Encoder l'image en format JPEG dans un tableau d'octets
			MatOfByte buffer = new MatOfByte();
			Imgcodecs.imencode(".jpg", frame, buffer);

			// Créer et retourner l'image JavaFX depuis le tableau d'octets
			return new Image(new ByteArrayInputStream(buffer.toArray()));
		} catch (Exception e) {
			System.err.println("Erreur lors de la conversion de l'image : " + e.getMessage());
			return null;
		}
	}

	public static String encodedData(byte[] data, Boolean isAudio) {
		String encodedData = Base64.getEncoder().encodeToString(data);
		if (isAudio) {
			encodedData = SeparatorAudio[0] + encodedData + "|" + calculateChecksum(encodedData) + SeparatorAudio[1];

		} else {
			encodedData = SeparatorVideo[0] + encodedData + "|" + calculateChecksum(encodedData) + SeparatorVideo[1];
		}
		return encodedData;
	}

	public static String calculateChecksum(String data) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
			StringBuilder hexString = new StringBuilder();
			for (byte b : hash) {
				hexString.append(String.format("%02x", b));
			}
			return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Erreur lors du calcul de la somme de contrôle : " + e.getMessage());
		}
	}

	public static void decodeAndPlayAudioLocally(String encodedAudio, int bufferSize, SourceDataLine speaker,
			AudioFormat format, DataLine.Info info) {
		try {
			byte[] audioBytes = Base64.getDecoder().decode(encodedAudio);
			int offset = 0;
			while (offset < audioBytes.length) {
				int bytesToWrite = Math.min(bufferSize, audioBytes.length - offset);
				speaker.write(audioBytes, offset, bytesToWrite);
				offset += bytesToWrite;
			}

		} catch (Exception e) {
			System.err.println("Erreur de lecture audio : " + e.getMessage());
		}
	}

	public static SourceDataLine initSpeaker(AudioFormat format) throws LineUnavailableException {
		SourceDataLine speaker = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format));
		speaker.open(format);
		speaker.start();
		return speaker;
	}

	public static void showMessage(String title, String message, String type) {
		Alert.AlertType alertType = (type != null && type.equals("success")) ? Alert.AlertType.INFORMATION
				: Alert.AlertType.ERROR;
		Alert alert = new Alert(alertType);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}

	public static String getResourcesPath() {
		return "/main/resources/";
	}

	public static JSONObject formateResp(String receivedMessage) {
		JSONObject jsonObject = new JSONObject(receivedMessage);

		// Initialisation du tableau unifié pour stocker les messages
		JSONArray messages = new JSONArray();

		// Vérification et traitement de la clé "datas"
		if (jsonObject.has("datas") && !jsonObject.get("datas").toString().isEmpty()) {
			Object datas = jsonObject.get("datas");
			if (datas instanceof JSONObject) {
				// Ajouter un seul objet message
				messages.put(datas);
			}
		}

		// Vérification et traitement de la clé "datasString"
		if (jsonObject.has("datasString") && !jsonObject.getString("datasString").isEmpty()) {
			JSONArray datasArray = new JSONArray(jsonObject.getString("datasString"));
			for (int i = 0; i < datasArray.length(); i++) {
				// Ajouter chaque message du tableau
				messages.put(datasArray.getJSONObject(i));
			}
		}

		// Création de la réponse finale
		JSONObject unifiedResponse = new JSONObject();
		unifiedResponse.put("success", jsonObject.getBoolean("success"));
		unifiedResponse.put("action", jsonObject.getString("action"));
		unifiedResponse.put("messages", messages);

		return unifiedResponse;
	}

	public static void enterToAction(KeyEvent event, Runnable funct) {
		if (event.getCode() == KeyCode.ENTER) {
			funct.run();
		}
	}

	public static String getLocalIpAddress() {
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface iface = interfaces.nextElement();
				if (iface.isLoopback() || !iface.isUp())
					continue;

				Enumeration<InetAddress> addresses = iface.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress addr = addresses.nextElement();
					if (addr instanceof Inet4Address) {
						return addr.getHostAddress();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "127.0.0.1"; // fallback
	}

}
