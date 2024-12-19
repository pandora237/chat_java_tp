package com.chat_java_tp;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message {
    private int idMessage;
    private int idSend;
    private int idReceive;
    private LocalDateTime createdAt;
    private String content;

    // Constructeur avec les paramètres nécessaires
    public Message( int idSend, int idReceive, String content) {
        this.idSend = idSend;
        this.idReceive = idReceive;
        this.content = content; 
    } 
    
    // Constructeur avec les paramètres nécessaires
    public Message(int idMessage, int idSend, int idReceive, String content, LocalDateTime createdAt) {
        this.idSend = idSend;
        this.idReceive = idReceive;
        this.content = content;
        this.createdAt=createdAt;
        this.idMessage=idMessage;
    } 
    
    // Getters et Setters
    public int getIdMessage() {
        return idMessage;
    }

    public void setIdMessage(int idMessage) {
        this.idMessage = idMessage;
    }

    public int getIdSend() {
        return idSend;
    }

    public void setIdSend(int idSend) {
        this.idSend = idSend;
    }

    public int getIdReceive() {
        return idReceive;
    }

    public void setIdReceive(int idReceive) {
        this.idReceive = idReceive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    // Méthode pour formater un message à partir d'une chaîne
    public static Message formateMessage(String text) {
        // Expression régulière pour capturer tous les champs dans le texte
        String regex = "idMessage=(\\d+), idSend=(\\d+), idReceive=(\\d+), content='(.*?)', createdAt=(.*?)\\}";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            // Extraction des valeurs
            int idMessage = Integer.parseInt(matcher.group(1));  
            int idSend = Integer.parseInt(matcher.group(2));     
            int idReceive = Integer.parseInt(matcher.group(3));  
            String content = matcher.group(4);                     
            String createdAtStr = matcher.group(5);               

            // Conversion de la chaîne 'createdAt' en LocalDateTime
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            LocalDateTime createdAt = LocalDateTime.parse(createdAtStr, formatter);
 
            return new Message(idMessage, idSend, idReceive, content, createdAt);
        } else { 
            return null;
        }
    }

    @Override
    public String toString() {
        return "Message{" +
                "idMessage=" + idMessage +
                ", idSend=" + idSend +
                ", idReceive=" + idReceive +
                ", createdAt=" + createdAt +
                ", content='" + content + '\'' +
                '}';
    }
}
