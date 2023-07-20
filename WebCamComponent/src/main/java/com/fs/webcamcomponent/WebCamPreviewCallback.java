package com.fs.webcamcomponent;

import javafx.scene.image.Image;

public interface WebCamPreviewCallback {
    void onError(String string);
    void onFrame(Image image);
}
