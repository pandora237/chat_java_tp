package com.chat_java_tp_client.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.chat_java_tp_client.ChatApp;
import com.chat_java_tp_client.helpers.AppState;
import com.chat_java_tp_client.helpers.Helpers;
import com.chat_java_tp_client.helpers.User;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class UserBlocController implements Initializable {

	private ChatApp mainApp;
	private AppState appState;

	@FXML
	private Label name;

	@FXML
	private Label statusUser;

	@FXML
	private HBox stautusUserBlock;

	@FXML
	private Label userName;
	private User selectedUser;

	private User user;

	private ChatController chatController;

	@FXML
	void actionSelectedUser(MouseEvent event) {
		appState.setSelecetedUser(user);
		mainApp.setAppSte(appState);
		chatController.ChangeUser(user);
	}

	public UserBlocController() {
	}

	private void updateDatas() {
		userName.setText(user.getUsername());
		name.setText(user.getFirstname() + " " + user.getLastname());
		statusUser.setText(user.getIsLogged() == 0 ? "Deconnecter" : "En ligne");
	}

	public ChatApp getMainApp() {
		return mainApp;
	}

	public void setMainApp(ChatApp mainApp) {
		this.mainApp = mainApp;
	}

	public AppState getAppState() {
		return appState;
	}

	public void setAppState(AppState appState) {
		this.appState = appState;
		this.selectedUser = appState.getSelecetedUser();
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
		updateDatas();
	}

	public ChatController getChatController() {
		return chatController;
	}

	public void setChatController(ChatController chatController) {
		this.chatController = chatController;
	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		// TODO Auto-generated method stub 
	}
}
