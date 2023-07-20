package com.fs.webcamcomponent;

import com.fs.webcamcomponent.hv.HikvisionOperation;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class CameraRecordVideoManager {


    private ImageView imageView;


    private static CameraRecordVideoManager instance;

    private WebCamOperationInterface webcamOperation;

    private CameraRecordVideoManager(){}


    public static CameraRecordVideoManager getInstance(){

        if (null == instance){
            synchronized (CameraRecordVideoManager.class){
                if (null == instance){
                    instance = new CameraRecordVideoManager();
                }
            }
        }
        return instance;
    }

    /**
     * 显示
     * @param config    配置
     * @param imageView 显示图片的控件
     */
    public void display(ParameterConfig config,ImageView imageView){
        this.imageView = imageView;
        if (null != webcamOperation){
            webcamOperation.release();
        }
        webcamOperation = new HikvisionOperation();
        webcamOperation.init(config, new WebCamInitCallback() {
            @Override
            public void onError(String errorMsg) {
                System.out.println("init error "+errorMsg);
            }

            @Override
            public void onSuccess() {
                startPreview();
            }
        });
    }

    private void startPreview(){
        webcamOperation.startPreview(imageView, new WebCamPreviewCallback() {
            @Override
            public void onError(String string) {
                System.err.println("preview error "+string);
            }

            @Override
            public void onFrame(Image image) {
                Platform.runLater(()->{imageView.setImage(image);});
            }
        });
    }

    public void startRecord(){
        if (null != webcamOperation){
            webcamOperation.startRecord();
        }

    }

    public void pauseRecord(){
        if (null != webcamOperation){
            webcamOperation.pauseRecord();
        }

    }


    public void stopRecord(){
        if (null != webcamOperation){
            webcamOperation.stopRecord();
        }

    }
    public void release(){
        if (null != webcamOperation){
            webcamOperation.release();
            webcamOperation = null;
        }
    }
}
