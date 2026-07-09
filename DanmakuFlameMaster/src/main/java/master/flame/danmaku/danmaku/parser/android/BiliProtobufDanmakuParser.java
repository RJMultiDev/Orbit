/*
 * Bilibili Protobuf Danmaku Parser
 * 用于解析新版 protobuf 格式的弹幕数据
 */

package master.flame.danmaku.danmaku.parser.android;

import android.graphics.Color;

import java.util.List;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.Duration;
import master.flame.danmaku.danmaku.model.AlphaValue;
import org.json.JSONArray;
import org.json.JSONException;
import android.text.TextUtils;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.DanmakuFactory;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.util.DanmakuUtils;

/**
 * Bilibili 新版 Protobuf 格式弹幕解析器
 */
public class BiliProtobufDanmakuParser extends BaseDanmakuParser {

    private float mDispScaleX;
    private float mDispScaleY;
    private List<?> mDanmakuSegments; // 存储 DmSegMobileReply 列表

    public BiliProtobufDanmakuParser() {
    }

    /**
     * 设置弹幕分段数据
     * 
     * @param segments DmSegMobileReply 列表
     */
    public void setDanmakuSegments(List<?> segments) {
        this.mDanmakuSegments = segments;
    }

    @Override
    public Danmakus parse() {
        if (mDanmakuSegments == null || mDanmakuSegments.isEmpty()) {
            return new Danmakus();
        }

        Danmakus result = new Danmakus();
        int index = 0;

        // 遍历所有分段
        for (Object segmentObj : mDanmakuSegments) {
            try {
                // 通过反射获取 elems 字段
                java.lang.reflect.Field elemsField = segmentObj.getClass().getField("elems");
                List<?> elems = (List<?>) elemsField.get(segmentObj);

                if (elems != null) {
                    // 遍历该分段中的所有弹幕
                    for (Object elemObj : elems) {
                        BaseDanmaku danmaku = parseDanmakuElem(elemObj, index++);
                        if (danmaku != null) {
                            result.addItem(danmaku);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    /**
     * 解析单条弹幕元素
     * 
     * @param elemObj DanmakuElem 对象
     * @param index   弹幕索引
     * @return BaseDanmaku 对象
     */
    private BaseDanmaku parseDanmakuElem(Object elemObj, int index) {
        try {
            Class<?> elemClass = elemObj.getClass();

            int progress = elemClass.getField("progress").getInt(elemObj);
            int mode = elemClass.getField("mode").getInt(elemObj);
            int fontsize = elemClass.getField("fontsize").getInt(elemObj);
            int color = elemClass.getField("color").getInt(elemObj);
            String content = (String) elemClass.getField("content").get(elemObj);

            if (content == null || content.isEmpty()) {
                return null;
            }

            if (mode == 8) {
                return null;
            }

            boolean enableAdvanced = sharedPreferences == null || sharedPreferences.getBoolean("player_danmaku_advanced_enable", true);

            if (!enableAdvanced && (mode == 7 || mode == 8)) {
                if (mode == 7 && content.startsWith("[") && content.endsWith("]")) {
                    try {
                        JSONArray jsonArray = new JSONArray(content);
                        if (jsonArray.length() >= 5) {
                            content = jsonArray.getString(4);
                        }
                    } catch (Exception e) {}
                }
                mode = 1;
            }

            if (sharedPreferences != null && mode != 7 && mode != 8
                    && sharedPreferences.getBoolean("player_danmaku_forceR2L", false)) {
                mode = 1;
            }

            BaseDanmaku item = mContext.mDanmakuFactory.createDanmaku(mode, mContext);
            if (item != null) {
                item.time = progress;

                DanmakuUtils.fillText(item, content);
                item.index = index;

                item.textSize = fontsize * (mDispDensity - 0.6f);

                item.textColor = color | 0xFF000000;
                item.textShadowColor = (color | 0xFF000000) <= Color.BLACK ? Color.WHITE : Color.BLACK;

                item.setTimer(mTimer);
                
                if (mode == 7 && content.startsWith("[") && content.endsWith("]")) {
                    try {
                        JSONArray jsonArray = new JSONArray(content);
                        String[] textArr = new String[jsonArray.length()];
                        for(int i=0; i<textArr.length; i++){
                            textArr[i] = jsonArray.getString(i);
                        }
                        
                        if (textArr.length >= 5) {
                            DanmakuUtils.fillText(item, textArr[4]);
                            float beginX = Float.parseFloat(textArr[0]);
                            float beginY = Float.parseFloat(textArr[1]);
                            float endX = beginX;
                            float endY = beginY;
                            String[] alphaArr = textArr[2].split("-");
                            int beginAlpha = (int) (AlphaValue.MAX * Float.parseFloat(alphaArr[0]));
                            int endAlpha = beginAlpha;
                            if (alphaArr.length > 1) {
                                endAlpha = (int) (AlphaValue.MAX * Float.parseFloat(alphaArr[1]));
                            }
                            long alphaDuration = (long) (Float.parseFloat(textArr[3]) * 1000);
                            long translationDuration = alphaDuration;
                            long translationStartDelay = 0;
                            float rotateY = 0, rotateZ = 0;
                            if (textArr.length >= 7 && !textArr[5].isEmpty()) {
                                rotateZ = Float.parseFloat(textArr[5]);
                                rotateY = Float.parseFloat(textArr[6]);
                            }
                            if (textArr.length >= 11 && !textArr[7].isEmpty()) {
                                endX = Float.parseFloat(textArr[7]);
                                endY = Float.parseFloat(textArr[8]);
                                if(!"".equals(textArr[9])){
                                    translationDuration = Integer.parseInt(textArr[9]);
                                }
                                if(!"".equals(textArr[10])){
                                    translationStartDelay = (long) (Float.parseFloat(textArr[10]));
                                }
                            }
                            if (beginX >= 0f && beginX <= 1f) {
                                beginX *= DanmakuFactory.BILI_PLAYER_WIDTH;
                            }
                            if (beginY >= 0f && beginY <= 1f) {
                                beginY *= DanmakuFactory.BILI_PLAYER_HEIGHT;
                            }
                            if (endX >= 0f && endX <= 1f) {
                                endX *= DanmakuFactory.BILI_PLAYER_WIDTH;
                            }
                            if (endY >= 0f && endY <= 1f) {
                                endY *= DanmakuFactory.BILI_PLAYER_HEIGHT;
                            }
                            item.duration = new Duration(alphaDuration);
                            item.rotationZ = rotateZ;
                            item.rotationY = rotateY;
                            mContext.mDanmakuFactory.fillTranslationData(item, beginX,
                                    beginY, endX, endY, translationDuration, translationStartDelay, mDispScaleX, mDispScaleY);
                            mContext.mDanmakuFactory.fillAlphaData(item, beginAlpha, endAlpha, alphaDuration);

                            if (textArr.length >= 12) {
                                if (!TextUtils.isEmpty(textArr[11]) && "true".equals(textArr[11])) {
                                    item.textShadowColor = Color.TRANSPARENT;
                                }
                            }
                            if (textArr.length >= 15) {
                                if (!"".equals(textArr[14])) {
                                    String motionPathString = textArr[14].substring(1);
                                    String[] pointStrArray = motionPathString.split("L");
                                    if (pointStrArray.length > 0) {
                                        float[][] points = new float[pointStrArray.length][2];
                                        for (int i = 0; i < pointStrArray.length; i++) {
                                            String[] pointArray = pointStrArray[i].split(",");
                                            points[i][0] = Float.parseFloat(pointArray[0]);
                                            points[i][1] = Float.parseFloat(pointArray[1]);
                                        }
                                        DanmakuFactory.fillLinePathData(item, points, mDispScaleX,
                                                mDispScaleY);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    DanmakuUtils.fillText(item, content);
                }

                item.setTimer(mTimer);
            }

            return item;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public BaseDanmakuParser setDisplayer(IDisplayer disp) {
        super.setDisplayer(disp);
        mDispScaleX = mDispWidth / DanmakuFactory.BILI_PLAYER_WIDTH;
        mDispScaleY = mDispHeight / DanmakuFactory.BILI_PLAYER_HEIGHT;
        return this;
    }
}
