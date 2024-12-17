package com.chat_java_tp;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;

import java.io.*;
import java.net.*;

public class ChatApp extends Application {
    private static final String SERVER_IP = "127.0.0.1"; // adresse IP du serveur
    private static final int PORT = 8081;
    
    private TextArea messageArea; // Zone pour afficher les messages
    private TextField inputField; // Champ pour saisir les messages
    private PrintWriter out; // Pour envoyer les messages au serveur
    
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage win) {
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

        // Mise en page
        VBox layout = new VBox(10, messageArea, inputField, sendButton);
        layout.setPadding(new Insets(10));
        
        Scene scene = new Scene(layout, 400, 300);
        win.setScene(scene);
        win.show();

        // Connexion au serveur
        new Thread(this::connectToServer).start();
    }

    // Connexion au serveur et gestion des messages
    private void connectToServer() {
        try (Socket socket = new Socket(SERVER_IP, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            this.out = out;

            // Thread pour écouter les messages entrants
            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        // Afficher le message reçu dans la zone de texte
                        messageArea.appendText("Serveur: " + message + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Méthode pour envoyer un message
    private void sendMessage() {
        String message = inputField.getText();
        if (!message.isEmpty() && out != null) {
            out.println(message); // Envoi du message au serveur
            messageArea.appendText("Vous: " + message + "\n");
            inputField.clear(); // Effacer le champ de saisie après envoi
        }
    }
}