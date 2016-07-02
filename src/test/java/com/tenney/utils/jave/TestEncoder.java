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
import java.util.Arrays;

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
	public void testGetinfo() throws EncoderException{
		Encoder encoder = new Encoder();
		System.out.println(Arrays.toString(encoder.getSupportedDecodingFormats()));
		System.out.println(Arrays.toString(encoder.getSupportedEncodingFormats()));
		System.out.println(Arrays.toString(encoder.getVideoEncoders()));
		System.out.println(Arrays.toString(encoder.getVideoDecoders()));
	}

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
        String parentPath="D:/template/" ;//Test.class.getResource("/").getPath();
      //转码
        VideoAttributes videoAttributes = new VideoAttributes(); //视频编码
        videoAttributes.setSize(new VideoSize(720, 480)); //分辨率
        videoAttributes.setPixFormat("yuv420p");
        videoAttributes.setCodec("libx264");
        videoAttributes.setRate("25");
        videoAttributes.setKeyint_min("60");
        videoAttributes.setGop_size("60");
        videoAttributes.setSc_threshold("0");
        
        AudioAttributes audioAttributes = new AudioAttributes(); //音频属性，必须设置，否则转码后没有声音
//        audioAttributes.setCodec("libfdk_aac");
        
        EncodingAttributes attributes = new EncodingAttributes(); //转码参数 
        attributes.setFormat("mp4"); //转成mp4格式
        attributes.setQscale("0.01");
        attributes.setStrict("-2");
        attributes.setVideoAttributes(videoAttributes);
        attributes.setAudioAttributes(audioAttributes);
        
        encoder.encode(new File(parentPath+"/test01.mpg"), new File(parentPath+"/test02.mp4"), attributes);//执行资源转码
    }

}
