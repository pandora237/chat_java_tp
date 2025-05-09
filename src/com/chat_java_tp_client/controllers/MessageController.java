package com.chat_java_tp_client.controllers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.chat_java_tp_client.helpers.Helpers;
import com.chat_java_tp_client.helpers.Message;
import com.chat_java_tp_client.helpers.User;
import java.io.FileInputStream;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class MessageController {
	private final String urlVideoImg = Helpers.getResourcesPath() + "img/videoCall.png";
	private ListView<VBox> messageContainer; // Conteneur principal pour ajouter les messages
	ChatController chatController;

	public MessageController(ListView messageContainer, ChatController chatController) {
		this.messageContainer = messageContainer;
		this.chatController = chatController;
	}

	// Méthode pour ajouter un message
	public void addMessage(Message message, User currentUser) {
		try {
			// Choisir le fichier FXML en fonction du type de message
			FXMLLoader loader;
			VBox messageBlock = null;
			boolean isCurrentUser = false;
			if (currentUser.getIdUser() == message.getIdSend()) {
				isCurrentUser = true;
			}
			switch (message.getType()) {
			case "simple":
				loader = new FXMLLoader(
						getClass().getResource(Helpers.getResourcesPath() + "fxml/ui/MessageSimple.fxml"));
				messageBlock = loader.load();
				setSimpleMessageData(messageBlock, message, isCurrentUser);
				break;

			case Helpers.audioType:
				loader = new FXMLLoader(
						getClass().getResource(Helpers.getResourcesPath() + "fxml/ui/MessageCall.fxml"));
				messageBlock = loader.load();
				setCallData(messageBlock, message, "Appel audio", isCurrentUser, false);
				break;

			case Helpers.videoType:
				loader = new FXMLLoader(
						getClass().getResource(Helpers.getResourcesPath() + "fxml/ui/MessageCall.fxml"));
				messageBlock = loader.load();
				setCallData(messageBlock, message, "Appel vidéo", isCurrentUser, true);
				break;

			case Helpers.sendFile:
				loader = new FXMLLoader(
						getClass().getResource(Helpers.getResourcesPath() + "fxml/ui/MessageFileSend.fxml"));
				messageBlock = loader.load();
				MessageFileSendController controller = loader.getController();
				controller.setChatController(chatController);
				controller.setMessage(message);
				setFileMessageData(messageBlock, message, isCurrentUser);
				break;
			case Helpers.emoji:
				try {
					loader = new FXMLLoader(
							getClass().getResource(Helpers.getResourcesPath() + "fxml/ui/MessageEmoji.fxml"));
					messageBlock = loader.load();
					ImageView imageEmoji = (ImageView) messageBlock.lookup("#ImageEmoji");

					String nameImg = message.getFileName();
					String filePath = Helpers.imageDirectoryPathEmoji + File.separator + nameImg;
					File imageFile = new File(filePath);

					if (imageFile.exists()) {
						Image image = new Image(new FileInputStream(imageFile));
						imageEmoji.setImage(image);
					} else {
						System.err.println("Fichier image non trouvé : " + filePath);
					}

				} catch (IOException e) {
					e.printStackTrace();
				}
				break;

			default:
				throw new IllegalArgumentException("Type de message inconnu : " + message.getType());
			}

			if (currentUser.getIdUser() == message.getIdSend()) {
				// Ajouter le bloc configuré au conteneur principal
				messageBlock.getStyleClass().add("rightMe");
			}
			messageContainer.getItems().add(messageBlock);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Méthode pour configurer les données d'un message simple
	private void setSimpleMessageData(VBox messageBlock, Message message, boolean isCurrentUser) {
		Label senderLabel = (Label) messageBlock.lookup("#senderLabel");
		Label contentLabel = (Label) messageBlock.lookup("#contentLabel");
		Label timeLabel = (Label) messageBlock.lookup("#timeLabel");

		senderLabel.setText(isCurrentUser ? "Vous" : message.getUsernameSend());
		contentLabel.setText(message.getContent());
		timeLabel.setText(message.getDateAdd().toString());
	}

	// Méthode pour configurer les données d'un appel
	private void setCallData(VBox messageBlock, Message message, String callType, boolean isCurrentUser,
			Boolean isVideo) {
		Label senderLabel = (Label) messageBlock.lookup("#senderLabel");
		Label contentLabel = (Label) messageBlock.lookup("#contentLabel");
		Label timeLabel = (Label) messageBlock.lookup("#timeLabel");
		ImageView iconCall = (ImageView) messageBlock.lookup("#iconCall");

		senderLabel.setText(isCurrentUser ? "Vous" : message.getUsernameSend());
		contentLabel.setText(callType + (message.getIdSend() == 1 ? " initié" : " reçu"));
		timeLabel.setText(message.getDateAdd().toString());
		URL imgUrl = getClass().getResource(urlVideoImg);
		if (isVideo && imgUrl != null) {
			iconCall.setImage(new Image(imgUrl.toExternalForm()));
		}

	}

	// Méthode pour configurer les données d'un fichier envoyé
	private void setFileMessageData(VBox messageBlock, Message message, boolean isCurrentUser) {

		Label senderLabel = (Label) messageBlock.lookup("#senderLabel");
		ImageView imageView = (ImageView) messageBlock.lookup("#fileImageView");
		Label timeLabel = (Label) messageBlock.lookup("#timeLabel");
		Label fileName = (Label) messageBlock.lookup("#fileName");

		senderLabel.setText(isCurrentUser ? "Vous" : message.getUsernameSend());
//		imageView.setImage(new Image("fxml/images/" + message.getFileName()));
		timeLabel.setText(message.getDateAdd());
		fileName.setText(message.getFileName());
	}

	public void removeAllChild() {
		messageContainer.getItems().clear();
	}
}
