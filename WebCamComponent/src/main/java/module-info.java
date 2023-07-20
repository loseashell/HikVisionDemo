module com.fs.webcamcomponent {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.bytedeco.ffmpeg;
    requires org.bytedeco.javacv;
    requires org.bytedeco.opencv;
    requires jna;
    requires examples;
    requires javafx.swing;
    requires javafx.media;



    opens com.fs.webcamcomponent to javafx.fxml, jna;
    exports com.fs.webcamcomponent;
    opens com.fs.webcamcomponent.hv.NetSDKDemo to jna;
    exports com.fs.webcamcomponent.hv.NetSDKDemo;
}