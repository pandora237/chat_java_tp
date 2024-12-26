package com.chat_java_tp_client.sound;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class Sound {

	private Clip clip;

	public static final String NOTIFICATION = "notification";
	public static final String CALL_AUDIO = "callAudio";
	public static final String CALL_VIDEO = "callVideo";

	// Chemins des fichiers audio MP3 non supporté par AudioSystem
	private final String notificationSound = "sound/notification.wav";
	private final String callAudioSound = "sound/notification.wav"; 
	private final String callVideoSound = "sound/notification.wav";

	// Constructeur par défaut
	public Sound() {
	}

	/**
	 * Joue un fichier audio en fonction du type et de la boucle.
	 * 
	 * @param type Le type de son à jouer (e.g., NOTIFICATION, CALL_AUDIO,
	 *             CALL_VIDEO).
	 * @param loop Détermine si le son doit être joué en boucle (true) ou non
	 *             (false).
	 */
	public void playSound(String type, boolean loop) {
		String filePath;
		switch (type) {
		case NOTIFICATION:
			filePath = notificationSound;
			break;
		case CALL_AUDIO:
			filePath = callAudioSound;
			break;
		case CALL_VIDEO:
			filePath = callVideoSound;
			break;
		default:
			System.err.println("Type de son invalide : " + type);
			return;
		}

		try {
			stopSound(); // Arrête tout son en cours de lecture

			File soundFile = new File(filePath);
			if (!soundFile.exists()) {
				System.err.println("Fichier audio introuvable : " + filePath);
				return;
			}

			AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
			clip = AudioSystem.getClip();
			clip.open(audioInputStream);

			if (loop) {
				clip.loop(Clip.LOOP_CONTINUOUSLY); // Lecture en boucle
			} else {
				clip.start(); // Lecture sans boucle
			}
		} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
			System.err.println("Erreur lors de la lecture du son : " + e.getMessage());
		}
	}

	/**
	 * Arrête le son si un son est en cours de lecture.
	 */
	public void stopSound() {
		if (clip != null && clip.isRunning()) {
			clip.stop();
			clip.close();
		}
	}

	/**
	 * Vérifie si un son est en cours de lecture.
	 * 
	 * @return true si un son est en cours de lecture, false sinon.
	 */
	public boolean isPlaying() {
		return clip != null && clip.isRunning();
	}

	/**
	 * Méthode principale pour démontrer la lecture et l'arrêt des sons.
	 * 
	 * @param args Arguments de la ligne de commande.
	 */
	public static void main(String[] args) {
		// Instanciation de la classe Sound
		Sound soundPlayer = new Sound();

		// Lecture du son de notification sans boucle
		System.out.println("Lecture du son de notification...");
		soundPlayer.playSound(NOTIFICATION, false);

		// Ajout d'un délai avant l'arrêt
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		// Arrêt du son
		System.out.println("Arrêt du son...");
		soundPlayer.stopSound();

		// Lecture du son de notification en boucle
		System.out.println("Lecture du son de notification en boucle...");
		soundPlayer.playSound(NOTIFICATION, true);

		// Ajout d'un délai avant l'arrêt
		try {
			Thread.sleep(5000); // 5 secondes de boucle
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		// Arrêt du son
		System.out.println("Arrêt du son...");
		soundPlayer.stopSound();
	}
}
