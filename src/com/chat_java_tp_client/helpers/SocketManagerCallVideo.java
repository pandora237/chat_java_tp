package com.chat_java_tp_client.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketManagerCallVideo extends SocketManagerCall {

	protected static String SERVER_IP;
	protected static int PORT_VIDEO_SEND;
	protected static int PORT_VIDEO_RECEIVE;

	protected Socket socket_video_send;
	protected InputStream in_video_send;
	protected PrintWriter out_video_send;

	protected Socket socket_video_receive;
	protected InputStream in_video_receive;
	protected PrintWriter out_video_receive;

	public SocketManagerCallVideo() {
		super();
		PORT_VIDEO_SEND = Integer.parseInt(config_env.get("PORT_VIDEO"));
		PORT_VIDEO_RECEIVE = PORT_VIDEO_SEND + 1;
	}

	@Override
	public InputStream getInputStreamSend() {
		return in_video_send;
	}

	@Override
	public PrintWriter getOutputStreamSend() {
		return out_video_send;
	}

	@Override
	public InputStream getInputStreamReceive() {
		return in_video_receive;
	}

	@Override
	public PrintWriter getOutputStreamReceive() {
		return out_video_receive;
	}

	@Override
	public void connect() {
//		super.connect();
		try {
			socket_video_send = new Socket(SERVER_IP, PORT_VIDEO_SEND);
			in_video_send = socket_video_send.getInputStream();
			out_video_send = new PrintWriter(socket_video_send.getOutputStream(), true);

			socket_video_receive = new Socket(SERVER_IP, PORT_VIDEO_RECEIVE);
			in_video_receive = socket_video_receive.getInputStream();
			out_video_receive = new PrintWriter(socket_video_receive.getOutputStream(), true);
			
			System.err.println(SERVER_IP);
			System.err.println(socket_video_receive);
			System.err.println(socket_video_send);
			System.err.println(in_video_send);
			System.err.println(out_video_receive);
		} catch (IOException e) {
			e.printStackTrace();
			close();
		}
	}

	@Override
	public void close() {
//		super.close();
		try {
			if (in_video_send != null)
				in_video_send.close();
			if (out_video_send != null)
				out_video_send.close();
			if (socket_video_send != null)
				socket_video_send.close();

			if (in_video_receive != null)
				in_video_receive.close();
			if (out_video_receive != null)
				out_video_receive.close();
			if (socket_video_receive != null)
				socket_video_receive.close();
		} catch (IOException e) {
			System.err.println("Erreur lors de la fermeture des connexions vidéo : " + e.getMessage());
		}
	}
}
