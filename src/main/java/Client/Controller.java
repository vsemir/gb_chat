package Client;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.*;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import jdk.internal.access.JavaIOFileDescriptorAccess;

public class Controller implements Initializable {

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nick;


    @FXML
    private AnchorPane clientPanel;
    @FXML
    private AnchorPane msgPanel;
    @FXML
    private TextField textField;
    @FXML
    private Button btnSend;
    @FXML
    private ListView<String> clientList;
    @FXML
    private TextArea textArea;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField loginField;
    @FXML
    private AnchorPane authPanel;

    private void connect() {
        try {
            this.socket = new Socket("localhost", 8189);
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            setAuth(false);
            closeClient(true);
            new Thread(() -> {
                try {
                    while (true) {
                        final String msgAuth = in.readUTF();
                        System.out.println("CLIENT: Received message: " + msgAuth);
                        if (msgAuth.startsWith("/authok")) {
                            setAuth(true);
                            closeClient(false);
                            nick = msgAuth.split("\\s")[1];
                            textArea.appendText("Успешная авторизация под ником " + nick + "\n");
                            break;
                        }
                        textArea.appendText(msgAuth + "\n");
                    }
                    while (true) {
                        String msgFromServer = in.readUTF();
                        System.out.println("CLIENT: Received message: " + msgFromServer);
                        if (msgFromServer.startsWith(nick)) {
                            msgFromServer = "[You] " + msgFromServer;
                        }
                        if ("/end".equalsIgnoreCase(msgFromServer)) {
                            break;
                        }
                        if (msgFromServer.startsWith("/clients")) {
                            final List<String> clients = new ArrayList<>(Arrays.asList(msgFromServer.split("\\s")));
                            clients.remove(0);
                            clientList.getItems().clear();
                            clientList.getItems().addAll(clients);
                            continue;
                        }
                        textArea.appendText(msgFromServer + "\n");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                } finally {
                    try {
                        setAuth(false);
                        socket.close();
                        nick = "";
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void setAuth(boolean isAuthSuccess) {
        authPanel.setVisible(!isAuthSuccess);
        authPanel.setManaged(!isAuthSuccess);

        msgPanel.setVisible(isAuthSuccess);
        msgPanel.setManaged(isAuthSuccess);

        clientPanel.setVisible(isAuthSuccess);
        clientPanel.setManaged(isAuthSuccess);
    }

    public void sendAuth(ActionEvent actionEvent) {
        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            System.out.println("CLIENT: Send auth message");
            out.writeUTF("/auth " + loginField.getText() + " " + passwordField.getText());
            loginField.clear();
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(ActionEvent actionEvent) {
        try {
            final String msg = textField.getText();
            System.out.println("CLIENT: Send message to server: " + msg);
            out.writeUTF(msg);
            textField.clear();
            textField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        connect();
    }

    public void selectClient(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            final String msg = textField.getText();
            String nickname = clientList.getSelectionModel().getSelectedItem();
            textField.setText("/w " + nickname + " " + msg);
            textField.requestFocus();
            textField.selectEnd();
        }
    }

    public void closeClient(boolean setAuth){
        Timer timer = new Timer();
//        TimerTask timerTask = new TimerTask() {
//            @Override
//            public void run() {
//
//            }
//        };
        timer.schedule(new TimerTask() {

            private JavaIOFileDescriptorAccess socket;

            @Override
            public void run() {
                if (setAuth){
                    try {
                        System.out.println(System.currentTimeMillis());
                        Platform.exit();
                        this.socket.close(FileDescriptor.err);
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
            }

        }, 2*60*1000);




    }
}