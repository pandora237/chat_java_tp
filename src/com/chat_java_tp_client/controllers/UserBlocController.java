package com.chat_java_tp_client.controllers;

import java.io.IOException;

import com.chat_java_tp_client.helpers.Helpers;
import com.chat_java_tp_client.helpers.User;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

public class UserBlocController {

	private ListView<VBox> userContainer; // Conteneur principal pour ajouter les messages

	public UserBlocController(ListView userContainer) {
		this.userContainer = userContainer;
	}

	public void addUser(User user) {
		try {
			FXMLLoader loader;
			VBox userBlock;
			loader = new FXMLLoader(getClass().getResource(Helpers.getResourcesPath() + "fxml/ui/userBlock.fxml"));
			userBlock = loader.load();
			setUserData(userBlock, user);
			if (user.getSexe().equals(User.sexeF)) {
				userBlock.getStyleClass().add("userFBlock");
			}
			if (user.getIsLogged() != 0) {
				userBlock.getStyleClass().add("statusOnline");
			}else {
				userBlock.getStyleClass().add("statusOffline");
			}
			userContainer.getItems().add(userBlock);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void setUserData(VBox userBlock, User user) {
		Label userNameLabel = (Label) userBlock.lookup("#userName");
		Label nameLabel = (Label) userBlock.lookup("#name");
		Label statusLabel = (Label) userBlock.lookup("#statusUser");

		userNameLabel.setText(user.getUsername());
		nameLabel.setText(user.getFirstname() + " " + user.getLastname());
		statusLabel.setText(user.getIsLogged() == 0 ? "Deconnecter" : "En ligne");
	}

	public void removeAllChild() {
		userContainer.getItems().clear();
	}
}
