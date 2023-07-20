package com.fs.webcamcomponent;

import javafx.scene.image.ImageView;

/**
 * 网络摄像头操作接口
 */
public interface WebCamOperationInterface {
    void init(ParameterConfig config, WebCamInitCallback callback);
    void startPreview(ImageView imageView,WebCamPreviewCallback callback);  //预览

    void startRecord(); //录制

    void pauseRecord(); //暂停录制
    void stopRecord();  //停止录制

    void release(); //释放资源

}
