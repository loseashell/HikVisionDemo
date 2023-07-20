package com.fs.webcamcomponent;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.embed.swing.SwingNode;

import javax.swing.*;
import java.awt.*;

public class WebCamApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(WebCamApplication.class.getResource("webcam_main.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 640, 480);
        stage.setTitle("Webcam!");
        stage.setScene(scene);
        SwingNode swingNode = new SwingNode();
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

    private void createAndSetSwingContent(
            final SwingNode swingNode
    ) {
        SwingUtilities.invokeLater(() ->
                swingNode.setContent(new JPanel())
        );
    }
}
