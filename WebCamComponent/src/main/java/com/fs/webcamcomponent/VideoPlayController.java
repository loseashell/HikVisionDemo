package com.fs.webcamcomponent;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaView;

public class VideoPlayController {

    private StackPane trackPane;

    private float totalLengthTime;

    public VideoPlayController(){

    }

    @FXML
    Slider sldProgress;
    @FXML
    Button btnPlay;

    @FXML
    MediaView mvDisplay;

    private SegmentVideoPlay videoPlay;

    @FXML
    protected void onPlayClick(){

        if (null == trackPane){
            trackPane = (StackPane) sldProgress.lookup(".track");
        }

        videoPlay = new SegmentVideoPlay("D:\\record\\dvr4", mvDisplay);
        videoPlay.init(new SegmentVideoPlay.SegmentVideoInitCallback() {
            @Override
            public void onInitFinish() {
                playVideo();
            }
        });
        sldProgress.setOnMouseDragReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                System.out.println("mouse drag release,slider value="+sldProgress.getValue());
                videoPlay.seekTo((int) sldProgress.getValue());

            }
        });
        sldProgress.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                System.out.println("mouse click  slider value = "+sldProgress.getValue());
                videoPlay.seekTo((int)sldProgress.getValue());
            }
        });
    }
    @FXML
    protected void pause(){
        if (null != videoPlay){
            videoPlay.pause();
        }
    }
    @FXML
    protected void restart(){
        if (null != videoPlay){
            videoPlay.restart();
        }
    }

    private void playVideo(){


        videoPlay.play(new SegmentVideoPlay.SegmentVideoPlayCallback() {
            @Override
            public void onProgress(int progress) {
                System.out.println("total progress = "+progress);
                sldProgress.setValue(progress);

                int percent = (int) (Math.ceil(progress*100/totalLengthTime));
                System.out.println("total percent = "+percent);
                String style = String.format("-fx-background-color: linear-gradient(to right, #2D819D %d%%, #969696 %d%%);",
                        percent, percent);
                trackPane.setStyle(style);
            }

            @Override
            public void onTotal(int total) {
                System.out.println("total = "+total);
                totalLengthTime = total;
                sldProgress.setMax(total);
            }
        });
    }
}
