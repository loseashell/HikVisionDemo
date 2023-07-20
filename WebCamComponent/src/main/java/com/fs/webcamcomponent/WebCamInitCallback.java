package com.fs.webcamcomponent;

import org.bytedeco.javacv.Frame;

public interface WebCamInitCallback {
    void onError(String errorMsg);
    void onSuccess();
}
