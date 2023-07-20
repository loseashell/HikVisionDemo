package com.fs.webcamcomponent;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.ArrayList;
import java.util.Comparator;

public class SegmentVideoPlay {
    private String dirPath;
    private MediaView mvDisplay;

    private int totalLength;  //所有视频总时长
    private int cumulativeVideoProgress;  //当前播放视频段的前面的视频段的时长总和

    private int currentVideoIndex;   //当前播放的视频段的索引

    private Media media;
    private MediaPlayer player;

    private ArrayList<VideoFileInfo> videoFileInfoList;

    private SegmentVideoPlayCallback playCallback;

    private ChangeListener<Duration> onPlayTimeChange;


    public SegmentVideoPlay(String dirPath, MediaView mvDisplay) {
        this.dirPath = dirPath;
        this.mvDisplay = mvDisplay;
    }


    public void init(SegmentVideoInitCallback callback){
        new Thread(() -> {
            videoFileInfoList = getVideoFilesInfo(dirPath);
            if (null != callback){
                Platform.runLater(()->callback.onInitFinish());
            }
        }).start();
    }

    private void _play(int index, final int segmentVideoProgress) {
        System.out.println("segment video  seek to "+ segmentVideoProgress);
        if (null == videoFileInfoList) {
            return;
        }
        if (index >= videoFileInfoList.size()) {
            cumulativeVideoProgress = 0;
            currentVideoIndex = 0;
            return;
        }

        if (index == currentVideoIndex && null !=player){
            player.seek(new Duration(segmentVideoProgress*1000));
            return;
        }

        if (null != player ){
            if (null != onPlayTimeChange){
                player.currentTimeProperty().removeListener(onPlayTimeChange);
            }
            player.stop();
        }

        player = new MediaPlayer(videoFileInfoList.get(index).media);

        currentVideoIndex = index;


        player.setOnEndOfMedia(() -> {
            System.out.println("play finish " + videoFileInfoList.get(index).getFileName());
            cumulativeVideoProgress += videoFileInfoList.get(index).lengthInTime;
            _play(index + 1, 0);

        });
        //start 播放进度监听
        onPlayTimeChange = (observableValue, oldValue, newValue) -> {
            int newValueSecond = (int)newValue.toSeconds();
            int add = newValueSecond - (int)oldValue.toSeconds();
            if (add > 0){
                System.out.println("add = "+add+",newValueSecond = "+newValueSecond+",currentTime = "+(int)player.getCurrentTime().toSeconds());
                playCallback.onProgress(cumulativeVideoProgress +newValueSecond); //跟新播放进度
            }
        };
        //end 播放进度监听
        mvDisplay.setMediaPlayer(player);
        player.setOnReady(()->{
            if (segmentVideoProgress > 0){
                player.seek(new Duration(segmentVideoProgress*1000));  //如果视频播放没有准备好，设置seek 无效，所以放在回调里
            }
        });
        player.currentTimeProperty().addListener(onPlayTimeChange);
        player.play();




    }

    public void play(SegmentVideoPlayCallback callback) {
        this.playCallback = callback;
        callback.onTotal(totalLength);
        _play(0, 0);
    }

    public void pause() {
        if (null != player){
            player.pause();
        }
    }

    public void restart(){
        if (null != player){
            player.play();
        }
    }

    /**
     * 在某时间播放
     * @param second  时间，单位秒
     */
    public void seekTo(int second) {

        if (videoFileInfoList.isEmpty() || second >totalLength) {
            return;
        }
        int localCurrentVideoProgress = 0; //某视频段的播放进度
        int localCurrentVideoIndex = 0;  //某视频段在列表中的索引位置
        int cumulativeTimeLength = 0; //累计时长
        boolean localPlay = false;
        for (int i = 0; i < videoFileInfoList.size(); i++) {
            cumulativeTimeLength += videoFileInfoList.get(i).lengthInTime;

            if (cumulativeTimeLength == second) {
                System.out.println("1 video file, name = " + videoFileInfoList.get(i).getFileName() + ", lengthInTime = " + videoFileInfoList.get(i).lengthInTime);
                cumulativeVideoProgress = second;
                localCurrentVideoIndex = i + 1;    //播放下一个
                localCurrentVideoProgress = 0;
                localPlay = true;
                break;
            } else if (cumulativeTimeLength > second) {
                System.out.println("2 video file, name = " + videoFileInfoList.get(i).getFileName() + ", lengthInTime = " + videoFileInfoList.get(i).lengthInTime);
                if (0 == i) {
                    cumulativeVideoProgress = 0;
                    localCurrentVideoIndex = 0;
                    localCurrentVideoProgress = second;
                    localPlay = true;
                    break;
                } else {
                    int time = videoFileInfoList.get(i).lengthInTime - (cumulativeTimeLength - second);
                    cumulativeVideoProgress = cumulativeTimeLength - videoFileInfoList.get(i).lengthInTime;
                    localCurrentVideoIndex = i;
                    localCurrentVideoProgress = time;
                    System.out.println("segment time = " + time);
                    localPlay = true;
                    break;
                }

            }
        }
        if (localPlay) {
            _play(localCurrentVideoIndex, localCurrentVideoProgress);
            playCallback.onProgress(second);
        }

    }


    public interface SegmentVideoPlayCallback {
        void onProgress(int progress);

        void onTotal(int total);
    }

    public interface SegmentVideoInitCallback{
        void onInitFinish();
    }

    /**
     * 获取视频文件信息列表
     * @param dirPath  文件夹路径
     * @return
     */
    private ArrayList<VideoFileInfo> getVideoFilesInfo(String dirPath) {
        ArrayList<VideoFileInfo> fileList = new ArrayList<>();
        File videoDir = new File(dirPath);
        if (videoDir.exists()) {
            //start 遍历文件夹获取视频文件的信息
            File[] files = videoDir.listFiles();
            for (File file : files) {
                if (!file.isDirectory()) {
                    VideoFileInfo fileInfo = new VideoFileInfo();
                    fileInfo.videoFilePath = file.getPath();
                  FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(file);
                    try {
                        frameGrabber.start();
                    } catch (FrameGrabber.Exception e) {
                        continue;
//                        throw new RuntimeException(e);
                    }
                    fileInfo.lengthInTime = (int) (frameGrabber.getLengthInTime() / (1000 * 1000));

                    //start 文件名中有特殊字符，需要进行转码
                     String mediaFilePath;
                    StringBuilder stringBuilder = new StringBuilder(fileInfo.videoFilePath);
                    String fileParent = stringBuilder.substring(0, stringBuilder.lastIndexOf("/") + 1);
                    String fileName = stringBuilder.substring(stringBuilder.lastIndexOf("/") + 1, stringBuilder.length());
                    System.out.println(String.format("fileParent = %1s,fileName = %2s", fileParent, fileName));
                    try {
                        mediaFilePath = "file:///" + fileParent + URLEncoder.encode(fileName, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println("filePath = " + mediaFilePath);
                    //end 文件名中有特殊字符，需要进行转码

                    media = new Media(mediaFilePath);
                    fileInfo.media = media;
                    fileInfo.lengthInTime = (int) (frameGrabber.getLengthInTime()/1000000);
                    totalLength +=fileInfo.lengthInTime;
                    fileList.add(fileInfo);
                   try {
                        frameGrabber.stop();
                        frameGrabber.release();

                    } catch (FrameGrabber.Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            //end 遍历文件夹获取视频文件的信息
            Comparator<VideoFileInfo> comparator = Comparator.comparing(VideoFileInfo::getFileName); //按照文件名排序
            fileList.sort(comparator);
        }

        return fileList;
    }


    class VideoFileInfo {
        public String videoFilePath;  //视频文件路径

        public Media media;
        public int lengthInTime;  //视频时长，单位秒

        public String getFileName() {
            StringBuffer stringBuffer = new StringBuffer(videoFilePath);
            return stringBuffer.substring(stringBuffer.lastIndexOf(File.separator));
        }
    }

}
