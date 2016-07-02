/*
 * JAVE - A Java Audio/Video Encoder (based on FFMPEG)
 * 
 * Copyright (C) 2008-2009 Carlo Pelliccia (www.sauronsoftware.it)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.tenney.utils.jave;

import java.io.Serializable;

/**
 * Attributes controlling the video encoding process.
 * 
 * @author Carlo Pelliccia
 */
public class VideoAttributes implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * This value can be setted in the codec field to perform a direct stream
	 * copy, without re-encoding of the audio stream.
	 */
	public static final String DIRECT_STREAM_COPY = "copy";
	
	/**
	 * The codec name for the encoding process. If null or not specified the
	 * encoder will perform a direct stream copy.
	 */
	private String codec = null;
	
	private String rate = null; //fps 设置帧频 缺省25
	private String keyint_min = null; //-g 1 -keyint_min 2 ( -keyint_min 60 -g 60 -sc_threshold 0) -g参数，这个可以调整关键帧密度,其中-keyint_min为最小关键帧间隔
	private String gop_size = null; //-g gop_size 设置图像组大小
	private String sc_threshold = null;//-sc_threshold这个命令会根据视频的运动场景，自动为你添加额外的I帧，所以会导致你编出来的视频关键帧间隔不是你设置的长度，这是只要将它设为0，问题就得到解决了
	/**
	 * The the forced tag/fourcc value for the video stream.
	 */
	private String tag = null;
	
	private String pixFormat=null;

	/**
	 * The bitrate value for the encoding process. If null or not specified a
	 * default value will be picked.
	 */
	private Integer bitRate = null;

	/**
	 * The frame rate value for the encoding process. If null or not specified a
	 * default value will be picked.
	 */
	private Integer frameRate = null;

	/**
	 * The video size for the encoding process. If null or not specified the
	 * source video size will not be modified.
	 */
	private VideoSize size = null;

	/**
	 * Returns the codec name for the encoding process.
	 * 
	 * @return The codec name for the encoding process.
	 */
	String getCodec() {
		return codec;
	}

	/**
	 * Sets the codec name for the encoding process. If null or not specified
	 * the encoder will perform a direct stream copy.
	 * 
	 * Be sure the supplied codec name is in the list returned by
	 * {@link Encoder#getVideoEncoders()}.
	 * 
	 * A special value can be picked from
	 * {@link VideoAttributes#DIRECT_STREAM_COPY}.
	 * 
	 * @param codec
	 *            The codec name for the encoding process.
	 */
	public void setCodec(String codec) {
		this.codec = codec;
	}

	/**
	 * Returns the the forced tag/fourcc value for the video stream.
	 * 
	 * @return The the forced tag/fourcc value for the video stream.
	 */
	String getTag() {
		return tag;
	}

	/**
	 * Sets the forced tag/fourcc value for the video stream.
	 * 
	 * @param tag
	 *            The the forced tag/fourcc value for the video stream.
	 */
	public void setTag(String tag) {
		this.tag = tag;
	}

	/**
	 * Returns the bitrate value for the encoding process.
	 * 
	 * @return The bitrate value for the encoding process.
	 */
	Integer getBitRate() {
		return bitRate;
	}

	/**
	 * Sets the bitrate value for the encoding process. If null or not specified
	 * a default value will be picked.
	 * 
	 * @param bitRate
	 *            The bitrate value for the encoding process.
	 */
	public void setBitRate(Integer bitRate) {
		this.bitRate = bitRate;
	}

	/**
	 * Returns the frame rate value for the encoding process.
	 * 
	 * @return The frame rate value for the encoding process.
	 */
	Integer getFrameRate() {
		return frameRate;
	}

	/**
	 * Sets the frame rate value for the encoding process. If null or not
	 * specified a default value will be picked.
	 * 
	 * @param frameRate
	 *            The frame rate value for the encoding process.
	 */
	public void setFrameRate(Integer frameRate) {
		this.frameRate = frameRate;
	}

	/**
	 * Returns the video size for the encoding process.
	 * 
	 * @return The video size for the encoding process.
	 */
	VideoSize getSize() {
		return size;
	}

	/**
	 * Sets the video size for the encoding process. If null or not specified
	 * the source video size will not be modified.
	 * 
	 * @param size
	 *            he video size for the encoding process.
	 */
	public void setSize(VideoSize size) {
		this.size = size;
	}

	
	public String getPixFormat() {
		return pixFormat;
	}

	public void setPixFormat(String pixFormat) {
		this.pixFormat = pixFormat;
	}
	

	/**
	 * rate的getter方法
	 * @return the rate
	 */
	public String getRate() {
		return rate;
	}

	/**
	 * rate的setter方法
	 * @param rate the rate to set
	 */
	public void setRate(String rate) {
		this.rate = rate;
	}

	/**
	 * keyint_min的getter方法
	 * @return the keyint_min
	 */
	public String getKeyint_min() {
		return keyint_min;
	}

	/**
	 * keyint_min的setter方法
	 * @param keyint_min the keyint_min to set
	 */
	public void setKeyint_min(String keyint_min) {
		this.keyint_min = keyint_min;
	}

	/**
	 * sc_threshold的getter方法
	 * @return the sc_threshold
	 */
	public String getSc_threshold() {
		return sc_threshold;
	}

	/**
	 * sc_threshold的setter方法
	 * @param sc_threshold the sc_threshold to set
	 */
	public void setSc_threshold(String sc_threshold) {
		this.sc_threshold = sc_threshold;
	}

	/**
	 * gop_size的getter方法
	 * @return the gop_size
	 */
	public String getGop_size() {
		return gop_size;
	}

	/**
	 * gop_size的setter方法
	 * @param gop_size the gop_size to set
	 */
	public void setGop_size(String gop_size) {
		this.gop_size = gop_size;
	}

	public String toString() {
		return getClass().getName() + "(codec=" + codec + ", bitRate="
				+ bitRate + ", frameRate=" + frameRate + ", size=" + size + ", pixFormat=" + pixFormat+")";
	}

}
