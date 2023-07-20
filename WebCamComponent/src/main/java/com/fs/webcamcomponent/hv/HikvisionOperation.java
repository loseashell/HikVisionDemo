package com.fs.webcamcomponent.hv;

import com.fs.webcamcomponent.*;
import com.fs.webcamcomponent.hv.NetSDKDemo.HCNetSDK;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.IntByReference;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.JavaFXFrameConverter;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class HikvisionOperation implements WebCamOperationInterface {

    private WebCamInitCallback initCallback;  //初始化回调
    private ParameterConfig config; //配置

    private WebCamPreviewCallback previewCallback; //预览回调

    private ImageView previewImageView;  //预览控件

    private static HCNetSDK hCNetSDK = null;  //海康SDK
    private static FRealDataCallBack fRealDataCallBack;//预览回调函数实现
    private static FExceptionCallBack_Imp fExceptionCallBack;  //异常回调

    private HCNetSDK.NET_DVR_DEVICEINFO_V30 m_strDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V30();

    private static PipedInputStream CAMERA_INPUT_STREAM;// 抓图输入流
    private static PipedOutputStream CAMERA_OUTPUT_STREAM;// 抓图输出流



    private Pointer pUser = null;
    private int lPreviewHandle;//预览句柄

   private int lUserID;

   private boolean displayImage = true;

   private boolean startRecord;

//   private CanvasFrame canvasFrame = new CanvasFrame("Webcam");

    private FrameRecordThread frameRecordThread ;

    private boolean record;

    private FrameRecordThread.FrameRecordParameter recordParameter;

    private int[] paramResolution;
    private int[] paramFrameRate;


    /**
     * 初始化
     * @param config    初始化配置参数
     * @param callback  初始化回调
     */
    @Override
    public void init(ParameterConfig config, WebCamInitCallback callback) {
        this.initCallback = callback;
        this.config = config;
        createSDKInstance();

        fRealDataCallBack = new FRealDataCallBack();
        fExceptionCallBack = new FExceptionCallBack_Imp();

        m_strDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V30();

        boolean initSuc = hCNetSDK.NET_DVR_Init();
        if (null != callback && true!=initSuc){
            callback.onError("init fail");
        }

        if (!hCNetSDK.NET_DVR_SetExceptionCallBack_V30(0,0,fExceptionCallBack,pUser)){
            return;
        }

        CAMERA_INPUT_STREAM = new PipedInputStream(2*1024);
        CAMERA_OUTPUT_STREAM = new PipedOutputStream();

        login();

        paramResolution = HikvisionUtils.getResolutionParam(config.width,config.height);  //获取跟摄像头相同或者相近的分辨率
        paramFrameRate = HikvisionUtils.getFrameRateParam(config.frameRate);  //获取跟摄像头相同或者相近的帧率
        System.out.println(String.format("param = %1d,width = %2d,height = %3d",paramResolution[0],paramResolution[1],paramResolution[2]));
        System.out.println(String.format("param = %1d, frameRate = %2d",paramFrameRate[0],paramFrameRate[1]));

        //start 设置录制配置参数
        recordParameter = new FrameRecordThread.FrameRecordParameter();
//        recordParameter.width = paramResolution[1];
//        recordParameter.height = paramResolution[2];
        recordParameter.width = config.width;
        recordParameter.height = config.height;
        recordParameter.frameRate = paramFrameRate[1];
        recordParameter.bitRate = config.bitRate;
        recordParameter.filePath = config.filePath;
        recordParameter.segmentTime = config.segmentTime;
        //end 设置录制配置参数
    }

    @Override
    public void startPreview(ImageView imageView, WebCamPreviewCallback previewCallback) {
        this.previewImageView = imageView;
        this.previewCallback = previewCallback;
        preview();
    }

    @Override
    public void startRecord() {
        if (null == frameRecordThread){
            frameRecordThread = new FrameRecordThread(recordParameter);
            frameRecordThread.start();
        }else {
            frameRecordThread.restartRecord();
        }
        record = true;
    }

    public void pauseRecord(){
        if (null !=frameRecordThread){
            record = false;
            frameRecordThread.pauseRecord();
        }
    }


    @Override
    public void stopRecord() {
        record = false;
        if (null != frameRecordThread){
            frameRecordThread.stopRecord();
        }
    }

    @Override
    public void release() {
        System.out.println("release");
        displayImage = false;
        if (lPreviewHandle > -1) {
            hCNetSDK.NET_DVR_StopRealPlay(lPreviewHandle); //停止预览
        }
        if (lUserID > -1) {
            hCNetSDK.NET_DVR_Logout_V30(lUserID); //注销登录
        }
        hCNetSDK.NET_DVR_Cleanup();//释放SDK

        if (null != frameRecordThread){
            frameRecordThread.stopRecord();
            frameRecordThread.release();
        }
    }

    /**
     * 动态库加载
     *摄像头的库
     * @return
     */
    private  boolean createSDKInstance() {
        if (hCNetSDK == null) {
            synchronized (HCNetSDK.class) {
                String strDllPath = "";
                try {
                    strDllPath = System.getProperty("user.dir") + "\\lib\\HV_lib\\HCNetSDK.dll";
                    hCNetSDK = (HCNetSDK) Native.loadLibrary(strDllPath, HCNetSDK.class);
                    System.out.println("load dll lib");
                } catch (Exception ex) {
                    System.out.println("loadLibrary: " + strDllPath + " Error: " + ex.getMessage());
                    initCallback.onError("create sdk instance fail hv_error,loadLibrary: " + strDllPath + " Error: " + ex.getMessage());

                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 登录摄像头
     */
    private void login(){
         new Thread(() -> {
             lUserID = hCNetSDK.NET_DVR_Login_V30(config.ip, (short) config.port, config.userName, config.password, m_strDeviceInfo);
             if (-1 == lUserID) {
                 int error = hCNetSDK.NET_DVR_GetLastError();
                 System.out.println("登录,错误代码：" + error);
                 Platform.runLater(()->{
                     initCallback.onError("login fail,hv_error code"+error);
                 });

             } else {
                 System.out.println("登录成功");
                 initCallback.onSuccess();
             }
         }).start();
    }

    /**
     * 预览
     */
    private void preview(){
        //start 预览的参数
        HCNetSDK.NET_DVR_PREVIEWINFO strClientInfo = new HCNetSDK.NET_DVR_PREVIEWINFO();
        strClientInfo.read();

        strClientInfo.lChannel = 1;  //通道号
        strClientInfo.dwStreamType = 0; //0-主码流，1-子码流，2-三码流，3-虚拟码流，以此类推
        strClientInfo.dwLinkMode = 4; //连接方式：0- TCP方式，1- UDP方式，2- 多播方式，3- RTP方式，4- RTP/RTSP，5- RTP/HTTP，6- HRUDP（可靠传输） ，7- RTSP/HTTPS，8- NPQ
        strClientInfo.bBlocked = 0;  //0- 非阻塞取流，1- 阻塞取流
//        strClientInfo.hPlayWnd = Integer.parseInt(null); // 播放句柄, 只能传AWT组件
        strClientInfo.byPreviewMode = 0;
        //end 预览的参数
//        new FrameGrabService(previewImageView).start();
        new FrameGrabThread(previewImageView).start();

        lPreviewHandle = hCNetSDK.NET_DVR_RealPlay_V40(lUserID,
                strClientInfo, fRealDataCallBack, null);  //开始预览


        if (lPreviewHandle <= -1) {
            int error;
            error = hCNetSDK.NET_DVR_GetLastError();
            System.out.println("预览失败,错误码：" + error);
            previewCallback.onError("preview fail hv_error code:"+error);
            return;
        }
        System.out.println("回调预览");


    }

    /**
     * 预览回调
     */
    class FRealDataCallBack implements HCNetSDK.FRealDataCallBack_V30 {

        @Override
        public void invoke(int lRealHandle, int dwDataType, ByteByReference pBuffer, int dwBufSize, Pointer pUser) {


            if (dwDataType == HCNetSDK.NET_DVR_SYSHEAD){  //系统头
                //视频压缩参数，https://open.hikvision.com/hardware/v2/%E7%BB%93%E6%9E%84%E4%BD%93/NET_DVR_COMPRESSION_INFO_V30.html
                System.out.println("系统头");
                HCNetSDK.NET_DVR_MULTI_STREAM_COMPRESSIONCFG_COND inCompressCfg = new HCNetSDK.NET_DVR_MULTI_STREAM_COMPRESSIONCFG_COND();
                inCompressCfg.dwSize = inCompressCfg.size();
                inCompressCfg.struStreamInfo.dwChannel = 1;
                inCompressCfg.write();
                Pointer lpInBuff = inCompressCfg.getPointer();
                IntByReference ibrBytesReturned = new IntByReference(0);//码流压缩参数
                HCNetSDK.NET_DVR_MULTI_STREAM_COMPRESSIONCFG streamCfg = new HCNetSDK.NET_DVR_MULTI_STREAM_COMPRESSIONCFG();
                Pointer lpOutBuff = streamCfg.getPointer();
                boolean success = hCNetSDK.NET_DVR_GetDeviceConfig(lUserID, HCNetSDK.NET_DVR_GET_MULTI_STREAM_COMPRESSIONCFG, 1
                        , lpInBuff, inCompressCfg.size(), ibrBytesReturned.getPointer(),lpOutBuff,streamCfg.size());  //获取设备配置参数
                streamCfg.read();
                if (success) {
                    System.out.println("获取压缩参数成功");
                    System.out.println("码流类型 ="+streamCfg.struStreamPara.byStreamType
                            +",视频编码类型 = "+streamCfg.struStreamPara.byVideoEncType);
                }else {
                    int error;
                    error = hCNetSDK.NET_DVR_GetLastError();
                    System.out.println("获取压缩参数失败，错误码"+error);
                }
                int videoFrameRateOption = streamCfg.struStreamPara.dwVideoFrameRate;   //获取流的帧率
                int resolutionOption = streamCfg.struStreamPara.byResolution;           //获取流的分辨率
                System.out.println("dwVideoFrameRate = "+videoFrameRateOption+"，byResolution = "+resolutionOption);

                streamCfg.struStreamPara.byResolution = new Integer(paramResolution[0]).byteValue();  //可选分辨率，19-HD720P(1280*720),20-XVGA(1280*960),27-1920*1080p,70-2560*1440
                streamCfg.struStreamPara.dwVideoFrameRate = paramFrameRate[0];   //可选帧率，0-全部（25帧），1-1/16，2-1/8，3-1/4，4-1/2，5-1，6-2，7-4，8-6，9-8，10-10，11-12，12-16，13-20，14-15，15-18，16－22，17-25

                streamCfg.write();

                boolean flag = hCNetSDK.NET_DVR_SetDeviceConfig(lUserID, HCNetSDK.NET_DVR_SET_MULTI_STREAM_COMPRESSIONCFG, 1
                        , lpInBuff, inCompressCfg.size(), ibrBytesReturned.getPointer(),lpOutBuff,streamCfg.size());  //设置设备视频流参数
                if (flag) {
                    System.out.println("设置压缩参数成功");
                    System.out.println("码流类型 ="+streamCfg.struStreamPara.byStreamType
                            +",视频编码类型 = "+streamCfg.struStreamPara.byVideoEncType);
                }else {
                    int error;
                    error = hCNetSDK.NET_DVR_GetLastError();
                    System.out.println("设置压缩参数失败，错误码"+error);

                    //start 设置摄像头返回的帧率
                   int[] frameRate =  HikvisionUtils.getFrameRateParamByOption(videoFrameRateOption);

                   if (null != frameRate){
                       if (null != frameRecordThread){
                           frameRecordThread.setFrameRate(frameRate[1]);
                       }else {
                           recordParameter.frameRate = frameRate[1];
                       }
                   }
                    //end 设置摄像头返回的帧率
  /*                   int[] resolution = HikvisionUtils.getResolutionByOption(resolutionOption);
                  if (null != resolution){
                       if (null != frameRecordThread){
                           frameRecordThread.setResolution(resolution[1],resolution[2]);
                       }else {
                           recordParameter.width = resolution[1];
                           recordParameter.height = resolution[2];
                       }
                   }*/

                }

            }else {
                //            System.out.println("CAMERA_OUTPUT_STREAM.write");
                if(dwBufSize < 1) {
                    System.out.println("dwBufsize < 1");
                    return;
                }

                try {
                    CAMERA_OUTPUT_STREAM.write(pBuffer.getPointer().getByteArray(0, dwBufSize)); //将视频流写入管道流
                } catch (IOException e) {
                    try {
                        CAMERA_OUTPUT_STREAM.close();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
    }

    class FExceptionCallBack_Imp implements  HCNetSDK.FExceptionCallBack{

        @Override
        public void invoke(int dwType, int lUserID, int lHandle, Pointer pUser) {
            System.out.println("exception error:" + dwType);
        }
    }

    /**
     * 视频帧抓取线程
     */
    class FrameGrabThread extends Thread {
        private ImageView displayIv;
        private FFmpegFrameGrabber frameGrabber = null;
        private JavaFXFrameConverter javaFXFrameConverter;

        public FrameGrabThread(ImageView imageView){
            this.displayIv = imageView;
            try { CAMERA_INPUT_STREAM.connect(CAMERA_OUTPUT_STREAM); } catch (IOException e) {
                previewCallback.onError("preview fail "+e.getMessage());
            }
            frameGrabber = new FFmpegFrameGrabber(CAMERA_INPUT_STREAM,0);
            javaFXFrameConverter = new JavaFXFrameConverter();

            frameGrabber.setImageWidth(config.width);    //设置宽
            frameGrabber.setImageHeight(config.height);  //设置高
            frameGrabber.setFrameRate(config.frameRate); //设置帧率
//            displayIv.imageProperty().bind(this.valueProperty());
            System.out.println("FrameGrabService ()  ");
        }

        @Override
        public void run() {
            try {
                frameGrabber.start();
            } catch (FrameGrabber.Exception e) {
                previewCallback.onError("frame grabber start fail "+e.getMessage());
                throw new RuntimeException(e);
            }
            System.out.println("task ()  ");
            while (displayImage){
                Frame frame = null;
                try {
//                    System.out.println("frameGrabber.grab()  ");
                    frame = frameGrabber.grab();  //抓取视频帧

                } catch (FrameGrabber.Exception e) {
                    Platform.runLater(()->{previewCallback.onError("frame grab  fail "+e.getMessage());});
                    System.err.println("frameGrabber.grab() error ");
                }
                if (null != frame){

                    if (record){
//                        System.out.println("frameList.offer  ,frameRecordThread is alive "+frameRecordThread.isAlive());
                        if (frameRecordThread.isAlive()){
                            frameRecordThread.addFrame(frame.clone()); //将视频帧放到录制队列
                        }else {  //如果线程挂掉了，关闭，并释放资源，重新开始一个线程
                            if (null != frameRecordThread){
                                frameRecordThread.stopRecord();
                                frameRecordThread.release();
                            }
                            frameRecordThread = null;
                            startRecord();
                        }
                    }

                    Image image = javaFXFrameConverter.convert(frame);
//                        updateValue(image);
                    previewCallback.onFrame(image);   //回调返回图像
                }
            }

            System.out.println("frameGrabber.grab()   ->>out ");
            try {
                CAMERA_INPUT_STREAM.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
