/**
 * 版权所有：Tenney
 * 项目名称: tenney-video-transcoder
 * 类名称:TestEncoder.java
 * 包名称:com.tenney.utils.jave
 * 
 * 创建日期:2015年8月15日 
 * 创建人:唐雄飞		
 * <author>      <time>      <version>    <desc>
 * 唐雄飞     下午4:35:54     	V1.0        N/A
 */

package com.tenney.utils.jave;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Test;

/**
 *类说明:<br/>
 *创建人:唐雄飞<br/>
 *创建日期:2015年8月15日<br/> 
 * 
 */
public class TestEncoder
{

    @Test
    public void testCutImage() throws FileNotFoundException, EncoderException
    {
        Encoder encoder = new Encoder();
        String parentPath="/Users/tenney/Desktop" ;//Test.class.getResource("/").getPath();
      //截图
        encoder.getImage(new File(parentPath+"/rmvb01.mp4"), new File(parentPath+"/rmvb01.jpg"), 1);
    }
    
    @Test
    public void testTranscode() throws IllegalArgumentException, InputFormatException, EncoderException{
        
        Encoder encoder = new Encoder();
        String parentPath="/Users/tenney/Desktop" ;//Test.class.getResource("/").getPath();
      //转码
        VideoAttributes videoAttributes = new VideoAttributes(); //视频编码
        videoAttributes.setSize(new VideoSize(720, 480)); //分辨率
        
        AudioAttributes audioAttributes = new AudioAttributes(); //音频属性，必须设置，否则转码后没有声音
        
        EncodingAttributes attributes = new EncodingAttributes(); //转码参数 
        attributes.setFormat("mp4"); //转成mp4格式
        attributes.setQscale("0");
        attributes.setStrict("-2");
        attributes.setVideoAttributes(videoAttributes);
        attributes.setAudioAttributes(audioAttributes);
        
        encoder.encode(new File(parentPath+"/rmvb01.rmvb"), new File(parentPath+"/rmvb01.mp4"), attributes);//执行资源转码
    }

}
