package com.fs.webcamcomponent;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import java.io.File;

/**
 * 笔记本自带摄像头线程
 * 预览和存储
 */
public class CameraThread extends Service<Image> {
    ImageView cameraView;
    private static long startTime = 0;
    private static long videoTS = 0;
    //    private static int captureWidth = 1920;
//    private static int captureHeight = 1080;
    private static int captureWidth = 640;
    private static int captureHeight = 480;
    private static final int BIT_RATE = 1000000; // 1 Mbps
    private static int FRAME_RATE = 30;
    private static int GOP_LENGTH_IN_FRAMES = 60;
    private int maxRecordingTimeInSeconds = 300; // 最大录制时长（5分钟）
    //    private int maxRecordingTimeInSeconds = 30; // 最大录制时长（5分钟）
    FFmpegFrameRecorder recorder;
    JavaFXFrameConverter converter;
    OpenCVFrameConverter.ToMat converterMat;

    public CameraThread(ImageView cameraView) {
        this.cameraView = cameraView;
        converterMat = new OpenCVFrameConverter.ToMat();
    }

    boolean recording = false;
    String path;

    public void startRecord(String path) {
        this.path = path;
        recording = true;
    }

    Object lock = new Object();

    public void createAndRecord(String path) {
        this.path = path;
        createAndRecord();
    }

    public void createAndRecord() {
        synchronized (lock) {
            // 停止当前录制器并保存文件
            System.out.println("调用视频录制");
            try {
                if (recorder != null) {
                    recorder.stop();
                    recorder.release();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("停止录制异常");
            }
            //开始录制
            //指定文件名、分辨率、音频通道数；初始化格式、编码器、比特率、采样率；分配AVPacket空间
            long begin = System.currentTimeMillis();
//        String newFilename = String.format("video_%d.mp4", ++splitIndex);
            String newFilename = "dvr_001.mp4";
            System.out.println("视频路径 = " + path + "/" + newFilename);
            File file = new File(path);
            System.out.println(file.exists());
            recorder = new FFmpegFrameRecorder(path + "/" + newFilename, captureWidth, captureHeight, 0);
            recorder.setInterleaved(true);
            recorder.setVideoOption("tune", "zerolatency");
            recorder.setVideoOption("preset", "ultrafast");
            recorder.setVideoOption("crf", "23");
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setVideoBitrate(BIT_RATE);
            recorder.setGopSize(GOP_LENGTH_IN_FRAMES);
            recorder.setFormat("mp4");
            recorder.setFrameRate(FRAME_RATE);
            try {
                recorder.start();
            } catch (FFmpegFrameRecorder.Exception e) {
                e.printStackTrace();
                System.out.println("启动录制异常");
            }
            System.out.println("录制视频启动时间 = " + (System.currentTimeMillis() - begin));
        }

    }

    public void stopRecord() {
        recording = false;
//        if (recorder != null) {
//            try {
//                recorder.stop();
//            } catch (FrameRecorder.Exception exception) {
//                exception.printStackTrace();
//            } finally {
//                try {
//                    recorder.close();
//                } catch (FrameRecorder.Exception e) {
//                    e.printStackTrace();
//                }
//            }
//            recorder = null;
//        }
    }

    boolean destroy = false;
    boolean showImg = true;
    Frame frame = null;
    OpenCVFrameGrabber grabber = null;
    CascadeClassifier faceCascade = null;

    @Override
    protected Task<Image> createTask() {
        Task<Image> task = new Task<Image>() {
            @Override
            protected void updateValue(Image image) {
                super.updateValue(image);
                try {
                    cameraView.setImage(image);
//                    showImg = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("CameraThread updateValue error");
                }
            }

            @Override
            protected void done() {
                super.done();
                System.out.println("CameraThread done");

                end();
            }

            @Override
            protected void failed() {
                super.failed();
                System.out.println("CameraThread failed");
                end();
            }

            @Override
            protected Image call() {
                System.out.println("摄像头线程");

//                File faceFile = new File(System.getProperty("user.dir") + "/haarcascade_frontalface_default.xml");
//                System.out.println("faceFile = "+faceFile.getPath());
//                if (faceFile.exists()) {
//                    faceCascade = new CascadeClassifier();
//                    faceCascade.load(faceFile.getPath());
//                }

                converter = new JavaFXFrameConverter();
                try {
                    grabber = new OpenCVFrameGrabber(0);
                    grabber.setFrameRate(FRAME_RATE);
                    grabber.setImageWidth(captureWidth);
                    grabber.setImageHeight(captureHeight);
                    grabber.start();

                    while (!destroy && grabber != null && (frame = grabber.grab()) != null) {
                        if (destroy) break;
                        try {
                            if (showImg && frame.imageHeight > 0 && frame.imageWidth > 0) {
                                showImg = false;
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (frame.imageHeight > 0 && frame.imageWidth > 0) {
                                            try {
//                                                if (faceCascade != null) {
//                                                    Mat matFrame = converterMat.convert(frame);
//                                                    org.bytedeco.opencv.opencv_core.Mat grayImage = new Mat();
//                                                    // 将帧转换为灰度图像
//                                                    org.bytedeco.opencv.global.opencv_imgproc.cvtColor(matFrame, grayImage, Imgproc.COLOR_BGR2GRAY);
//                                                    org.bytedeco.opencv.global.opencv_imgproc.equalizeHist(grayImage, grayImage);
//
//                                                    RectVector faceDetections = new RectVector();
////                                                faceCascade.detectMultiScale(grayImage, faceDetections);
//                                                    faceCascade.detectMultiScale(grayImage, faceDetections, 1.2, 5, 0, new Size(50, 50), new Size(500, 500));
//
//                                                    for (int i = 0; i < faceDetections.size(); i++) {
//                                                        Rect rect = faceDetections.get(i);
//                                                        org.bytedeco.opencv.global.opencv_imgproc.rectangle(matFrame, rect.tl(), rect.br(), new Scalar(0, 255, 0, 0));
//                                                    }
//                                                }
                                                Image image = converter.convert(frame);
                                                cameraView.setImage(image);

                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                System.out.println("转换异常");
                                            }
                                        }
                                        showImg = true;
                                    }
                                });
                            } else {
//                                System.out.println("阻塞，丢弃一帧");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (recording) {
                            if (recorder == null) {
                                createAndRecord();
                            }
                            synchronized (lock) {
                                try {
                                    if (frame.imageHeight > 0 && frame.imageWidth > 0) {
                                        recorder.record(frame);

                                    } else {
                                        System.out.println("frame width and height = 0");
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                        } else {
                            if (recorder != null) {
                                recorder.stop();
                                recorder.close();
                                recorder = null;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("摄像头异常了");
                    end();
                    return null;
//                    System.out.println("--- 摄像头异常了 ---");
                } finally {
                    System.out.println("摄像头 finally");
                    releseCamera();
                }
                return null;
            }
        };
        return task;
    }

    public void end() {
        destroy = true;

    }
    public void releseCamera(){
        if (grabber != null) {
            try {
                grabber.stop();
            } catch (FrameGrabber.Exception exception) {
                exception.printStackTrace();
            }
            try {
                grabber.release();
            } catch (FrameGrabber.Exception e) {
                e.printStackTrace();
            }
            grabber = null;
        }

        grabber = null;
        if (recorder != null) {
            try {
                recorder.stop();

            } catch (FrameRecorder.Exception e) {
                e.printStackTrace();
            }
            try {
                recorder.close();
            } catch (FrameRecorder.Exception e) {
                e.printStackTrace();
            }
            try {
                recorder.release();
            } catch (FFmpegFrameRecorder.Exception e) {
                e.printStackTrace();
            }
            recorder = null;
        }
    }

    public void setDestroy(boolean destroy) {
        this.destroy = destroy;
    }
}
