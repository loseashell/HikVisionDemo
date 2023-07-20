package com.fs.webcamcomponent;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 录制线程
 */
public class FrameRecordThread extends Thread{

    private FrameRecordParameter parameter;

    private FFmpegFrameRecorder recorder;
    private static int  count = 1;

    private boolean record;


    private ConcurrentLinkedQueue<Frame> frameList ;

    public void addFrame(Frame frame){
        frameList.offer(frame);
    }

    public void startRecord(){
        try {
            recorder.start();
            record = true;
        } catch (FrameRecorder.Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void pauseRecord(){
        record = false;
    }

    public void restartRecord(){
        record = true;
    }

    public void stopRecord(){
        try {
            record = false;
            recorder.stop();
        } catch (FrameRecorder.Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void release(){
        try {
            recorder.release();
        } catch (FrameRecorder.Exception e) {
            throw new RuntimeException(e);
        }
    }


    public void setFrameRate(int frameRate){
        if (null != recorder){
            recorder.setFrameRate(frameRate);
        }
    }

    public void setResolution(int width,int height){
        if (null != recorder){
            recorder.setImageWidth(width);
            recorder.setImageHeight(height);
        }
    }

    public FrameRecordThread(FrameRecordParameter parameter){
        this.parameter = parameter;
        frameList = new ConcurrentLinkedQueue<>();
        String filePath = parameter.filePath + File.separator +count+ "_dvr_[%03d].mp4";
        System.out.println("视频路径 = " + filePath);
        File file = new File(filePath);
        System.out.println("record file path"+file.exists());
        recorder = new FFmpegFrameRecorder(file, parameter.width, parameter.height, 0);

        recorder.setFormat("segment");
        recorder.setOption("segment_time", String.valueOf(parameter.segmentTime)); //设置视频时长，单位秒
        //生成模式：live（实时生成）,cache（边缓存边生成，只支持m3u8清单文件缓存）
        recorder.setOption("segment_list_flags", "live");
        recorder.setOption("reset_timestamps","1");   //设置时间戳，否则无法播放
        recorder.setInterleaved(true);
        recorder.setVideoOption("tune", "zerolatency");
        recorder.setVideoOption("preset", "ultrafast");
        recorder.setVideoOption("crf", "23");
        recorder.setGopSize(60);
        recorder.setFrameRate(parameter.frameRate);//设置帧率
        recorder.setVideoBitrate(parameter.bitRate); //设置比特率
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);//视频编码方式
        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);//设置音频编码


//            recorder.setFormat("mp4");
        try {
            recorder.start();
            record = true;
        } catch (FrameRecorder.Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {


        while (true){
            if (record){
                //start 从列表中获取视频帧，并录制
                if (!frameList.isEmpty()){
                    Frame frame = frameList.poll();
                    try {
                        if (null != frame){
                            recorder.record(frame);
                        }
                    } catch (FrameRecorder.Exception e) {
                        System.err.println("record error ");
                        e.printStackTrace();
                        return;
                    }
                }
                //end 从列表中获取视频帧，并录制
            }
        }
    }


    public static class FrameRecordParameter{
        public int width;
        public int height;
        public String filePath;
        public int segmentTime;
        public int frameRate;
        public int bitRate;
    }
}
