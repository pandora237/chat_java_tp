package com.chat_java_tp_client.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketManagerMessage {

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	public BufferedReader getInputStream() {
		return in;
	}

	public PrintWriter getOutputStream() {
		return out;
	}

	private static final String SERVER_IP = "127.0.0.1";
	protected static int PORT = 8081;

	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;

	public void connect() throws IOException {
		socket = new Socket(SERVER_IP, PORT);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(socket.getOutputStream(), true);
	}

	public void close() throws IOException {
		if (socket != null) {
			socket.close();
		}
	}
}
