package com.fs.webcamcomponent;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class WebcamController {


    public WebcamController(){

    }
    @FXML
    private ImageView previewIv;
    @FXML
    protected void onStartRecordClick(){
        CameraRecordVideoManager.getInstance().startRecord();
    }
    @FXML
    protected void onPauseRecordClick(){
        CameraRecordVideoManager.getInstance().pauseRecord();
    }
    @FXML
    protected void onPreviewClick(){
        ParameterConfig config = new ParameterConfig();
        config.ip = "192.168.1.64";
        config.port = 8000;
        config.userName = "admin";
        config.password = "fs123456";
        config.width = 640;
        config.height = 480;
        config.frameRate = 20;
        config.bitRate = 1000000;
        config.filePath = "D:\\record";
        config.segmentTime = 1800;
        CameraRecordVideoManager.getInstance().display(config,previewIv);
    }
    @FXML
    protected void onGetFrame(){
/*        GetFrame getFrame = new GetFrame("D:\\record",200);
        getFrame.getFrame(new GetFrame.OnFrameCallback() {
            @Override
            public void onFrame(Image image) {
                previewIv.setImage(image);
            }
        });*/
        Application videoPlay = new VideoPlayApplication();
        try {
            videoPlay.start(new Stage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public void release()
    {
        CameraRecordVideoManager.getInstance().stopRecord();
        CameraRecordVideoManager.getInstance().release();
    }

    @FXML
    protected void onStopRelease(){
        release();
    }
}
