module chat_java_tp_client {
	requires java.base;
	requires javafx.graphics;
	requires java.sql;
	requires javafx.controls;
	requires org.json;
	requires javafx.fxml;
	requires java.desktop;
	requires opencv;
	requires javafx.base; 
	requires javafx.media;

	exports com.chat_java_tp_client; 
	opens com.chat_java_tp_client.controllers to javafx.fxml;
}