package com.fs.webcamcomponent.hv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HikvisionUtils {


    //视频压缩参数，https://open.hikvision.com/hardware/v2/%E7%BB%93%E6%9E%84%E4%BD%93/NET_DVR_COMPRESSION_INFO_V30.html
    /**
     * 分辨率{可选参数，宽，高}
     **/
    static List<int[]> optionResolution =  Arrays.asList(new int[]{19, 1280, 720}, new int[]{20, 1280, 960}, new int[]{27, 1920, 1080}, new int[]{70, 2560, 1440});
    /**
     * 分辨率{可选参数，帧率},不使用 1-1/16，2-1/8，3-1/4，4-1/2 ，这几个帧率
     * 可选帧率，0-全部（25帧），1-1/16，2-1/8，3-1/4，4-1/2，5-1，6-2，7-4，8-6，9-8，10-10，11-12，12-16，13-20，14-15，15-18，16－22，17-25
     **/
    static List<int[]> optionFrameRate =  Arrays.asList(new int[]{5, 1}, new int[]{6, 2}, new int[]{7, 4}, new int[]{8, 6}, new int[]{9, 8}, new int[]{10, 10}, new int[]{11, 12}, new int[]{12, 16}, new int[]{13, 20}, new int[]{14, 15}, new int[]{15, 18}, new int[]{16, 22}, new int[]{17, 25});
    int[] ii = {1, 1};

    private HikvisionUtils() {

    }

    /**
     * 通过可选参数 返回分辨率
     * @param param 可选参数
     * @return 分辨率
     */
    public static int[] getResolutionByOption(int param){
        for(int[] item:optionResolution){
            if (param == item[0]){
                return item;
            }
        }
        return null;
    }

    /**
     * 找到最相近的分辨率参数
     *
     * @param width  宽
     * @param height 高
     * @return 参数
     */
    public static int[] getResolutionParam(int width, int height) {
        int i = Integer.MAX_VALUE;
        int[] result = null;
        ArrayList<int[]> sameWidth = new ArrayList<>(); //宽相同的参数
        for (int[] element : optionResolution) {
            int j = Math.abs(element[1] - width);
            if (i == j) {
                sameWidth.add(result);
            } else if (j < i) {
                sameWidth.clear();
                result = element;
                i = j;
            }
        }

        if (sameWidth.isEmpty()) {
            return result;
        } else {
            int l = Integer.MAX_VALUE;
            int[] resultl = null;
            for (int[] element : sameWidth) {
                int j = Math.abs(element[2] - height);
                if (j <= l) {
                    resultl = element;
                    l = j;
                }
            }
            return resultl;
        }
    }

    /**
     * 通过可选参数 找到对应的帧率
     * @param option 可选参数
     * @return 帧率
     */
    public static int[] getFrameRateParamByOption(int option){
        for(int[] item:optionFrameRate){
            if (option == item[0]){
                return item;
            }
        }
        return null;
    }

    /**
     * 找到最相近的帧率参数
     *
     * @param frameRate 帧率
     * @return 帧率参数
     */
    public static int[] getFrameRateParam(int frameRate) {
        int i = Integer.MAX_VALUE;
        int[] result = null;
        for (int[] element : optionFrameRate) {
            int j = Math.abs(element[1] - frameRate);
            if (j < i) {
                result = element;
                j = i;
            }
        }
        return result;
    }
}
