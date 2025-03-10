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

	private static String SERVER_IP;
	protected static int PORT;

	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;

	public SocketManagerMessage() {
		ConfigEnv config_env = new ConfigEnv();
		SERVER_IP = config_env.get("SERVER_IP_MESSAGE");
		PORT = Integer.parseInt(config_env.get("PORT_MESSAGE"));
	}

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
