package com.chat_java_tp_client.helpers;

import java.io.*;
import java.net.Socket;

public class SocketManagerCall2 implements AutoCloseable {

	protected static String SERVER_IP;
	protected static int PORT_SEND;
	protected static int PORT_RECEIVE;

	protected Socket socket_send;
	protected DataInputStream in_send;
	protected DataOutputStream out_send;

	protected Socket socket_receive;
	protected DataInputStream in_receive;
	protected DataOutputStream out_receive;

	ConfigEnv config_env;

	public SocketManagerCall2() {
		config_env = new ConfigEnv();
		SERVER_IP = config_env.get("SERVER_IP_AUDIO_VIDEO");
		PORT_SEND = Integer.parseInt(config_env.get("PORT_AUDIO"));
		PORT_RECEIVE = PORT_SEND + 1;
	}

	public void connect() {
		try {
			socket_send = new Socket(SERVER_IP, PORT_SEND);
			in_send = new DataInputStream(socket_send.getInputStream());
			out_send = new DataOutputStream(socket_send.getOutputStream());

			socket_receive = new Socket(SERVER_IP, PORT_RECEIVE);
			in_receive = new DataInputStream(socket_receive.getInputStream());
			out_receive = new DataOutputStream(socket_receive.getOutputStream());

			System.out.println("Connecté à " + SERVER_IP + " sur les ports " + PORT_SEND + " et " + PORT_RECEIVE);
		} catch (IOException e) {
			System.err.println("Erreur lors de la connexion : " + e.getMessage());
			close();
		}
	}

	public DataInputStream getInputStreamSend() {
		return in_send;
	}

	public DataOutputStream getOutputStreamSend() {
		return out_send;
	}

	public DataInputStream getInputStreamReceive() {
		return in_receive;
	}

	public DataOutputStream getOutputStreamReceive() {
		return out_receive;
	}

	@Override
	public void close() {
		try {
			if (in_send != null)
				in_send.close();
			if (out_send != null)
				out_send.close();
			if (socket_send != null)
				socket_send.close();

			if (in_receive != null)
				in_receive.close();
			if (out_receive != null)
				out_receive.close();
			if (socket_receive != null)
				socket_receive.close();
		} catch (IOException e) {
			System.err.println("Erreur lors de la fermeture des sockets : " + e.getMessage());
		}
	}
}
