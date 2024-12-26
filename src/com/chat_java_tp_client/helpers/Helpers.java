package com.chat_java_tp_client.helpers;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import javafx.scene.image.Image;

public class Helpers {
	// commun server
	public static final String audioFile = "send_file";
	public static final String audioType = "audio_call";
	public static final String videoType = "video_call";
	// end commun

	public static final String[] SeparatorVideo = { "START_VIDEO", "END_VIDEO " };
	public static final String[] SeparatorAudio = { "START_AUDIO", "END_AUDIO " };

	public static Image matToImage(Mat frame) {
		try {
			MatOfByte buffer = new MatOfByte();
			Imgcodecs.imencode(".bmp", frame, buffer); // Encode l'image au format BMP
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

		} else if (data instanceof byte[]) {
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

	public static void playAudioLocally(String encodedAudio, int bufferSize) {
		try {
			byte[] audioBytes = Base64.getDecoder().decode(encodedAudio);
			AudioFormat format = new AudioFormat(22050, 16, 1, true, false);
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

			if (!AudioSystem.isLineSupported(info)) {
				System.err.println("Le format audio n'est pas supporté.");
				return;
			}

			try (SourceDataLine speaker = (SourceDataLine) AudioSystem.getLine(info)) {
				speaker.open(format);
				speaker.start();

				byte[] buffer = new byte[bufferSize];
				int offset = 0;

				while (offset < audioBytes.length) {
					int bytesToWrite = Math.min(bufferSize, audioBytes.length - offset);
					System.arraycopy(audioBytes, offset, buffer, 0, bytesToWrite);
					speaker.write(buffer, 0, bytesToWrite);
					offset += bytesToWrite;
				}

				speaker.drain();
			}
		} catch (IllegalArgumentException e) {
			System.err.println("Erreur lors du décodage Base64 des données audio : " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur de lecture audio : " + e.getMessage());
		}
	}

}
