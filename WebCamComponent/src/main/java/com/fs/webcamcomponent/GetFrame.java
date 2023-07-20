package com.fs.webcamcomponent;

import javafx.application.Platform;
import javafx.scene.image.Image;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.JavaFXFrameConverter;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class GetFrame {


    private String dirPath;
    private long second;


    public GetFrame(String dirPath, long second) {
        this.dirPath = dirPath;
        this.second = second;
    }

    public void getFrame(OnFrameCallback callback) {
        new Thread(() -> {
             ArrayList<VideoFileInfo> list = getVideoFilesInfo(dirPath);
             Image result = getFrame(list,second);
             if (null != callback){
                 Platform.runLater(()->{ callback.onFrame(result); });
             }
        }).start();
    }

    class VideoFileInfo {
        public File videoFile;
        public long lengthInTime;

        public String getFileName() {
            return videoFile.getName();
        }
    }


    private ArrayList<VideoFileInfo> getVideoFilesInfo(String dirPath) {
        ArrayList<VideoFileInfo> fileList = new ArrayList<>();
        File videoDir = new File(dirPath);
        if (videoDir.exists()) {
            File[] files = videoDir.listFiles();
            for (File file : files) {
                if (!file.isDirectory()) {
                    VideoFileInfo fileInfo = new VideoFileInfo();
                    fileInfo.videoFile = file;
                    FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(file);
                    try {
                        frameGrabber.start();
                    } catch (FrameGrabber.Exception e) {
                        throw new RuntimeException(e);
                    }
                    fileInfo.lengthInTime = frameGrabber.getLengthInTime() / (1000 * 1000);
                    fileList.add(fileInfo);
                    try {
                        frameGrabber.stop();
                        frameGrabber.release();

                    } catch (FrameGrabber.Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            Comparator<VideoFileInfo> comparator = Comparator.comparing(VideoFileInfo::getFileName);
            fileList.sort(comparator);
            for (int i = 0; i < fileList.size(); i++) {
                System.out.println("video file name = " + fileList.get(i).getFileName() + ", lengthInTime = " + fileList.get(i).lengthInTime);
            }
        }

        return fileList;
    }

    private Image getFrame(ArrayList<VideoFileInfo> fileInfoList, long second) {
        if (fileInfoList.isEmpty()) {
            return null;
        }
        long cumulativeTimeLength = 0;
        Image result = null;
        for (int i = 0; i < fileInfoList.size(); i++) {
            cumulativeTimeLength += fileInfoList.get(i).lengthInTime;
            if (cumulativeTimeLength == second) {
                System.out.println("frame in video file, name = " + fileInfoList.get(i).getFileName() + ", lengthInTime = " + fileInfoList.get(i).lengthInTime);
                FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(fileInfoList.get(i).videoFile);
                try {
                    frameGrabber.start();
//                    int lengthInFrames = frameGrabber.getLengthInFrames();
//                    frameGrabber.setFrameNumber(lengthInFrames - 1);
                    frameGrabber.setVideoTimestamp(second*1000*1000);
                    Frame frame = frameGrabber.grab();
                    JavaFXFrameConverter fxFrameConverter = new JavaFXFrameConverter();
                    result = fxFrameConverter.convert(frame);
                    return result;
                } catch (FrameGrabber.Exception e) {
                    throw new RuntimeException(e);
                }

            } else if (cumulativeTimeLength > second) {
                System.out.println("frame in video file, name = " + fileInfoList.get(i).getFileName() + ", lengthInTime = " + fileInfoList.get(i).lengthInTime);
                try {
                    FFmpegFrameGrabber  frameGrabber = FFmpegFrameGrabber.createDefault(fileInfoList.get(i).videoFile);
                    frameGrabber.start();
                    long time = 0;
                    if (0==i){
                        time = second;
                    }else {
                        time = fileInfoList.get(i).lengthInTime -(cumulativeTimeLength - second);
                    }
//                    frameGrabber.setFrameNumber((int) (time * frameGrabber.getFrameRate()));
                    System.out.println("time = "+time);
                    frameGrabber.setVideoTimestamp(time*1000*1000);
                    Frame frame = frameGrabber.grab();
                    JavaFXFrameConverter fxFrameConverter = new JavaFXFrameConverter();
                    result = fxFrameConverter.convert(frame);
                    return result;
                } catch (FrameGrabber.Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return result;
    }


    public interface OnFrameCallback {
        void onFrame(Image image);
    }
}
