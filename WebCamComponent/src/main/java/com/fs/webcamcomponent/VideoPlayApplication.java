package com.fs.webcamcomponent;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class VideoPlayApplication extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(VideoPlayApplication.class.getResource("video_play.fxml"));
        Scene scene = new Scene(loader.load(),600,400);
        primaryStage.setTitle("Video Play");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
