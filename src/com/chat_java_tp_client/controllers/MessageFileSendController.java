package com.chat_java_tp_client.controllers;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.chat_java_tp_client.helpers.Helpers;
import com.chat_java_tp_client.helpers.Message;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

public class MessageFileSendController implements Initializable {
	Message message;
	ChatController chatController;
	private Button downloadBtn;

	@FXML
	private Hyperlink hyperLink;

	@FXML
	private VBox messageBlock;

	@FXML
	private Label senderLabel;

	@FXML
	private Label timeLabel;

	@FXML
	void onActionBtn(MouseEvent event) {
		System.out.println(message.getIdMessage());
		chatController.signalAskFile(message.getIdMessage());
	}

	@FXML
	void onActionLink(ActionEvent event) {
		Helpers.openFileFolder(Helpers.FILE_DOWNLOAD);
	}

	public ChatController getChatController() {
		return chatController;
	}

	public void setChatController(ChatController chatController) {
		this.chatController = chatController;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {

	}

}
