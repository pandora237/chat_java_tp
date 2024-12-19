package com.chat_java_tp;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.geometry.Insets;

import java.io.*;
import java.net.*;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChatApp extends Application {
	private static final String SERVER_IP = "127.0.0.1"; // adresse IP du serveur
	private static final int PORT = 8081;
	public static final String FILE_DOWNLOAD = "downloads/";

	private User currentUser;

	private TextArea messageArea; // Zone pour afficher les messages
	private TextField inputField; // Champ pour saisir les messages
	private PrintWriter out; // Pour envoyer les messages au serveur
	private volatile boolean running = true; // Flag pour indiquer si le client est actif

	public static void main(String[] args) {
		Application.launch(args);
	}

	@Override
	public void start(Stage win) {
		currentUser = new User(1, "Alongamo", "Franky", "Fr_Al", "franky@gmail.com", "+237 681050506");
//        currentUser= new User(1, "Doe", "Jhon", "J_D", "test@gmail.com", "+237 620603562");

		// Configuration de l'interface graphique
		win.setTitle("Chat Application");

		// Zone pour afficher les messages
		messageArea = new TextArea();
		messageArea.setEditable(false);
		messageArea.setWrapText(true);

		// Champ de saisie du message
		inputField = new TextField();
		inputField.setPromptText("Entrez votre message");

		// Bouton d'envoi
		Button sendButton = new Button("Envoyer");
		sendButton.setOnAction(e -> sendMessage());

		Button fileButton = new Button("Envoyer un fichier");
		fileButton.setOnAction(e -> sendFile());

		// Mise en page
		VBox layout = new VBox(10, messageArea, inputField, sendButton, fileButton);
		layout.setPadding(new Insets(10));

		Scene scene = new Scene(layout, 400, 300);
		win.setScene(scene);
		win.show();

		// Connexion au serveur
		new Thread(this::connectToServer).start();
	}

	// Connexion au serveur et gestion des messages
	private void connectToServer() {
		try {
			Socket socket = new Socket(SERVER_IP, PORT);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);

			// Thread pour écouter les messages entrants
			new Thread(() -> {
				try {
					String response;
					while (running && (response = in.readLine()) != null) {
						JSONObject jsonObject = new JSONObject(response);
						if ("get_messages".equals(jsonObject.get("action"))) {

						}

						if ("send_file".equals(jsonObject.get("action"))) {
							JSONObject fileData = jsonObject.getJSONObject("datas");
							String fileName = fileData.getString("fileName");
							byte[] fileContent = Base64.getDecoder().decode(fileData.getString("fileContent"));

							File downloadedFile = new File(FILE_DOWNLOAD + fileName);
							downloadedFile.getParentFile().mkdirs();// Créer le dossier si nécessaire

							try (FileOutputStream fos = new FileOutputStream(downloadedFile)) {
								fos.write(fileContent);
								messageArea.appendText("Fichier reçu : " + fileName + " (enregistré dans downloads)\n");
							} catch (IOException e) {
								e.printStackTrace();
							}
						} else {
							JSONObject mess = jsonObject.getJSONObject("datas");
							System.out.println("Réception : " + mess.getString("content") + " :::::::: " + response);
							messageArea.appendText("Serveur: " + mess.getString("content") + "\n");
						}

					}
				} catch (IOException e) {
					if (running)
						e.printStackTrace();
				} finally {
					try {
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).start();

//			List<JSONObject> oldMessages = getMessages(in);
//			System.out.println("oldMessages : " + oldMessages);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Méthode pour envoyer un message
	private void sendMessage() {
		String message = inputField.getText();
		if (!message.isEmpty() && out != null) {
			JSONObject messageDatas = new JSONObject();
			messageDatas.put("idSend", currentUser.getId());
			messageDatas.put("idReceive", 2);
			messageDatas.put("content", message);
			out.println(messageDatas.toString()); // Envoi du message au serveur
			messageArea.appendText(currentUser.getPrenom() + " : " + message + "\n");
			inputField.clear();
		}
	}

	private List<JSONObject> getMessages(BufferedReader in) {
		try {
			if (out != null) {
				// Envoyer une requête pour récupérer les anciens messages
				JSONObject request = new JSONObject();
				request.put("__ACTION", "get_messages");
				request.put("action", "retrieveData");
				request.put("userId", currentUser.getId());
				out.println(request.toString()); // Envoi de la requête

				// Lire la réponse du serveur
				String response = in.readLine();

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return List.of(); // Retourner une liste vide en cas d'erreur
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

	private void sendFile() {
		FileChooser fileChooser = new FileChooser();
		File file = fileChooser.showOpenDialog(null);

		if (file != null && file.exists()) {
			try (FileInputStream fis = new FileInputStream(file)) {
				byte[] fileBytes = fis.readAllBytes();
				JSONObject fileMessage = new JSONObject();
				fileMessage.put("action", "send_file");
				fileMessage.put("fileName", file.getName());
				fileMessage.put("fileSize", fileBytes.length);
				fileMessage.put("fileContent", Base64.getEncoder().encodeToString(fileBytes));

				out.println(fileMessage.toString()); // Envoi du fichier au serveur
				messageArea.appendText("Fichier envoyé : " + file.getName() + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// Arrêt propre de l'application
	@Override
	public void stop() {
		running = false; // Arrêter le thread de lecture
		if (out != null) {
			out.close();
		}
	}
}