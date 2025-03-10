package com.chat_java_tp_client.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketManagerCall {

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	public InputStream getInputStream() {
		return in;
	}

	public PrintWriter getOutputStream() {
		return out;
	}

	protected static String SERVER_IP;
	protected static int PORT;

	protected Socket socket;
	protected InputStream in;
	protected PrintWriter out;

	public SocketManagerCall() {
		ConfigEnv config_env = new ConfigEnv();
		SERVER_IP = config_env.get("SERVER_IP_AUDIO_VIDEO");
		PORT = Integer.parseInt(config_env.get("PORT_AUDIO_VIDEO"));
	}

	public void connect() throws IOException {
		socket = new Socket(SERVER_IP, PORT);
		in = socket.getInputStream();
		out = new PrintWriter(socket.getOutputStream(), true);
	}

	public void close() throws IOException {
		if (socket != null) {
			socket.close();
		}
	}
}
