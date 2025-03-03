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

	protected static final String SERVER_IP = "127.0.0.1";
	protected static int PORT = 8082;

	protected Socket socket;
	protected InputStream in;
	protected PrintWriter out;

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
