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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * Main class of the package. Instances can encode audio and video streams.
 * 
 * @author Carlo Pelliccia
 */
public class Encoder {

	/**
	 * This regexp is used to parse the ffmpeg output about the supported
	 * formats.
	 */
	private static final Pattern FORMAT_PATTERN = Pattern
			.compile("^\\s*([D ])([E ])\\s+([\\w,]+)\\s+.+$");

	private Logger logger = Logger.getLogger(Encoder.class);
	/**
	 * This regexp is used to parse the ffmpeg output about the included
	 * encoders/decoders.
	 */
	private static final Pattern ENCODER_DECODER_PATTERN = Pattern.compile(
			"^\\s*([D ])([E ])([AVS]).{3}\\s+(.+)$", Pattern.CASE_INSENSITIVE);

	/**
	 * This regexp is used to parse the ffmpeg output about the ongoing encoding
	 * process.
	 */
	private static final Pattern PROGRESS_INFO_PATTERN = Pattern.compile(
			"\\s*(\\w+)\\s*=\\s*(\\S+)\\s*", Pattern.CASE_INSENSITIVE);

	/**
	 * This regexp is used to parse the ffmpeg output about the size of a video
	 * stream.
	 */
	private static final Pattern SIZE_PATTERN = Pattern.compile(
			"(\\d+)x(\\d+)", Pattern.CASE_INSENSITIVE);

	/**
	 * This regexp is used to parse the ffmpeg output about the frame rate value
	 * of a video stream.
	 */
	private static final Pattern FRAME_RATE_PATTERN = Pattern.compile(
			"([\\d.]+)\\s+(?:fps|tb\\(r\\))", Pattern.CASE_INSENSITIVE);

	/**
	 * This regexp is used to parse the ffmpeg output about the bit rate value
	 * of a stream.
	 */
	private static final Pattern BIT_RATE_PATTERN = Pattern.compile(
			"(\\d+)\\s+kb/s", Pattern.CASE_INSENSITIVE);

	/**
	 * This regexp is used to parse the ffmpeg output about the sampling rate of
	 * an audio stream.
	 */
	private static final Pattern SAMPLING_RATE_PATTERN = Pattern.compile(
			"(\\d+)\\s+Hz", Pattern.CASE_INSENSITIVE);

	/**
	 * This regexp is used to parse the ffmpeg output about the channels number
	 * of an audio stream.
	 */
	private static final Pattern CHANNELS_PATTERN = Pattern.compile(
			"(mono|stereo)", Pattern.CASE_INSENSITIVE);

	/**
	 * This regexp is used to parse the ffmpeg output about the success of an
	 * encoding operation.
	 */
	private static final Pattern SUCCESS_PATTERN = Pattern.compile(
			"^\\s*video\\:\\S+\\s+audio\\:\\S+.*\\s+global headers\\:\\S+.*$",
			Pattern.CASE_INSENSITIVE);

	/**
	 * The locator of the ffmpeg executable used by this encoder.
	 */
	private FFMPEGLocator locator;

	/**
	 * It builds an encoder using a {@link DefaultFFMPEGLocator} instance to
	 * locate the ffmpeg executable to use.
	 */
	public Encoder() {
		this.locator = new DefaultFFMPEGLocator();
	}

	/**
	 * It builds an encoder with a custom {@link FFMPEGLocator}.
	 * 
	 * @param locator
	 *            The locator picking up the ffmpeg executable used by the
	 *            encoder.
	 */
	public Encoder(FFMPEGLocator locator) {
		this.locator = locator;
	}

	/**
	 * Returns a list with the names of all the audio decoders bundled with the
	 * ffmpeg distribution in use. An audio stream can be decoded only if a
	 * decoder for its format is available.
	 * 
	 * @return A list with the names of all the included audio decoders.
	 * @throws EncoderException
	 *             If a problem occurs calling the underlying ffmpeg executable.
	 */
	public String[] getAudioDecoders() throws EncoderException {
		ArrayList<String> res = new ArrayList<String>();
		FFMPEGExecutor ffmpeg = locator.createExecutor();
		ffmpeg.addArgument("-formats");
		RBufferedReader reader = null;
		try {
			ffmpeg.execute();
			reader = new RBufferedReader(new InputStreamReader(ffmpeg
					.getInputStream()));
			String line;
			boolean evaluate = false;
			while ((line = reader.readLine()) != null) {
				if (line.trim().length() == 0) {
					continue;
				}
				if (evaluate) {
					Matcher matcher = ENCODER_DECODER_PATTERN.matcher(line);
					if (matcher.matches()) {
						String decoderFlag = matcher.group(1);
						String audioVideoFlag = matcher.group(3);
						if ("D".equals(decoderFlag)
								&& "A".equals(audioVideoFlag)) {
							String name = matcher.group(4);
							res.add(name);
						}
					} else {
						break;
					}
				} else if (line.trim().equals("Codecs:")) {
					evaluate = true;
				}
			}
		} catch (IOException e) {
			throw new EncoderException(e);
		} finally {
		    if(reader != null)
                try{reader.close();}
                catch (IOException e){}
			ffmpeg.destroy();
		}
		int size = res.size();
		String[] ret = new String[size];
		for (int i = 0; i < size; i++) {
			ret[i] = (String) res.get(i);
		}
		return ret;
	}

	/**
	 * Returns a list with the names of all the audio encoders bundled with the
	 * ffmpeg distribution in use. An audio stream can be encoded using one of
	 * these encoders.
	 * 
	 * @return A list with the names of all the included audio encoders.
	 * @throws EncoderException
	 *             If a problem occurs calling the underlying ffmpeg executable.
	 */
	public String[] getAudioEncoders() throws EncoderException {
		ArrayList<String> res = new ArrayList<String>();
		FFMPEGExecutor ffmpeg = locator.createExecutor();
		ffmpeg.addArgument("-formats");
		RBufferedReader reader = null;
		try {
			ffmpeg.execute();
			reader = new RBufferedReader(new InputStreamReader(ffmpeg
					.getInputStream()));
			String line;
			boolean evaluate = false;
			while ((line = reader.readLine()) != null) {
				if (line.trim().length() == 0) {
					continue;
				}
				if (evaluate) {
					Matcher matcher = ENCODER_DECODER_PATTERN.matcher(line);
					if (matcher.matches()) {
						String encoderFlag = matcher.group(2);
						String audioVideoFlag = matcher.group(3);
						if ("E".equals(encoderFlag)
								&& "A".equals(audioVideoFlag)) {
							String name = matcher.group(4);
							res.add(name);
						}
					} else {
						break;
					}
				} else if (line.trim().equals("Codecs:")) {
					evaluate = true;
				}
			}
		} catch (IOException e) {
			throw new EncoderException(e);
		} finally {
		    if(reader != null)
                try{reader.close();}
                catch (IOException e){}
			ffmpeg.destroy();
		}
		int size = res.size();
		String[] ret = new String[size];
		for (int i = 0; i < size; i++) {
			ret[i] = (String) res.get(i);
		}
		return ret;
	}

	/**
	 * Returns a list with the names of all the video decoders bundled with the
	 * ffmpeg distribution in use. A video stream can be decoded only if a
	 * decoder for its format is available.
	 * 
	 * @return A list with the names of all the included video decoders.
	 * @throws EncoderException
	 *             If a problem occurs calling the underlying ffmpeg executable.
	 */
	public String[] getVideoDecoders() throws EncoderException {
		ArrayList<String> res = new ArrayList<String>();
		FFMPEGExecutor ffmpeg = locator.createExecutor();
		ffmpeg.addArgument("-formats");
		RBufferedReader reader = null;
		try {
			ffmpeg.execute();
			reader = new RBufferedReader(new InputStreamReader(ffmpeg
					.getInputStream()));
			String line;
			boolean evaluate = false;
			while ((line = reader.readLine()) != null) {
				if (line.trim().length() == 0) {
					continue;
				}
				if (evaluate) {
					Matcher matcher = ENCODER_DECODER_PATTERN.matcher(line);
					if (matcher.matches()) {
						String decoderFlag = matcher.group(1);
						String audioVideoFlag = matcher.group(3);
						if ("D".equals(decoderFlag)
								&& "V".equals(audioVideoFlag)) {
							String name = matcher.group(4);
							res.add(name);
						}
					} else {
						break;
					}
				} else if (line.trim().equals("Codecs:")) {
					evaluate = true;
				}
			}
		} catch (IOException e) {
			throw new EncoderException(e);
		} finally {
		    if(reader != null)
                try{reader.close();}
                catch (IOException e){}
			ffmpeg.destroy();
		}
		int size = res.size();
		String[] ret = new String[size];
		for (int i = 0; i < size; i++) {
			ret[i] = (String) res.get(i);
		}
		return ret;
	}

	/**
	 * Returns a list with the names of all the video encoders bundled with the
	 * ffmpeg distribution in use. A video stream can be encoded using one of
	 * these encoders.
	 * 
	 * @return A list with the names of all the included video encoders.
	 * @throws EncoderException
	 *             If a problem occurs calling the underlying ffmpeg executable.
	 */
	public String[] getVideoEncoders() throws EncoderException {
		ArrayList<String> res = new ArrayList<String>();
		FFMPEGExecutor ffmpeg = locator.createExecutor();
		ffmpeg.addArgument("-formats");
		RBufferedReader reader = null;
		try {
			ffmpeg.execute();
			reader = new RBufferedReader(new InputStreamReader(ffmpeg
					.getInputStream()));
			String line;
			boolean evaluate = false;
			while ((line = reader.readLine()) != null) {
				if (line.trim().length() == 0) {
					continue;
				}
				if (evaluate) {
					Matcher matcher = ENCODER_DECODER_PATTERN.matcher(line);
					if (matcher.matches()) {
						String encoderFlag = matcher.group(2);
						String audioVideoFlag = matcher.group(3);
						if ("E".equals(encoderFlag)
								&& "V".equals(audioVideoFlag)) {
							String name = matcher.group(4);
							res.add(name);
						}
					} else {
						break;
					}
				} else if (line.trim().equals("Codecs:")) {
					evaluate = true;
				}
			}
		} catch (IOException e) {
			throw new EncoderException(e);
		} finally {
		    if(reader != null)
                try{reader.close();}
                catch (IOException e){}
			ffmpeg.destroy();
		}
		int size = res.size();
		String[] ret = new String[size];
		for (int i = 0; i < size; i++) {
			ret[i] = (String) res.get(i);
		}
		return ret;
	}

	/**
	 * Returns a list with the names of all the file formats supported at
	 * encoding time by the underlying ffmpeg distribution. A multimedia file
	 * could be encoded and generated only if the specified format is in this
	 * list.
	 * 
	 * @return A list with the names of all the supported file formats at
	 *         encoding time.
	 * @throws EncoderException
	 *             If a problem occurs calling the underlying ffmpeg executable.
	 */
	public String[] getSupportedEncodingFormats() throws EncoderException {
		ArrayList<String> res = new ArrayList<String>();
		FFMPEGExecutor ffmpeg = locator.createExecutor();
		ffmpeg.addArgument("-formats");
		RBufferedReader reader = null;
		try {
			ffmpeg.execute();
			reader = new RBufferedReader(new InputStreamReader(ffmpeg
					.getInputStream()));
			String line;
			boolean evaluate = false;
			while ((line = reader.readLine()) != null) {
				if (line.trim().length() == 0) {
					continue;
				}
				if (evaluate) {
					Matcher matcher = FORMAT_PATTERN.matcher(line);
					if (matcher.matches()) {
						String encoderFlag = matcher.group(2);
						if ("E".equals(encoderFlag)) {
							String aux = matcher.group(3);
							StringTokenizer st = new StringTokenizer(aux, ",");
							while (st.hasMoreTokens()) {
								String token = st.nextToken().trim();
								if (!res.contains(token)) {
									res.add(token);
								}
							}
						}
					} else {
						break;
					}
				} else if (line.trim().equals("File formats:")) {
					evaluate = true;
				}
			}
		} catch (IOException e) {
			throw new EncoderException(e);
		} finally {
		    if(reader != null)
                try{reader.close();}
                catch (IOException e){}
			ffmpeg.destroy();
		}
		int size = res.size();
		String[] ret = new String[size];
		for (int i = 0; i < size; i++) {
			ret[i] = (String) res.get(i);
		}
		return ret;
	}

	/**
	 * Returns a list with the names of all the file formats supported at
	 * decoding time by the underlying ffmpeg distribution. A multimedia file
	 * could be open and decoded only if its format is in this list.
	 * 
	 * @return A list with the names of all the supported file formats at
	 *         decoding time.
	 * @throws EncoderException
	 *             If a problem occurs calling the underlying ffmpeg executable.
	 */
	public String[] getSupportedDecodingFormats() throws EncoderException {
		ArrayList<String> res = new ArrayList<String>();
		FFMPEGExecutor ffmpeg = locator.createExecutor();
		ffmpeg.addArgument("-formats");
		
		RBufferedReader reader = null;
		try {
			ffmpeg.execute();
			reader = new RBufferedReader(new InputStreamReader(ffmpeg
					.getInputStream()));
			String line;
			boolean evaluate = false;
			while ((line = reader.readLine()) != null) {
				if (line.trim().length() == 0) {
					continue;
				}
				if (evaluate) {
					Matcher matcher = FORMAT_PATTERN.matcher(line);
					if (matcher.matches()) {
						String decoderFlag = matcher.group(1);
						if ("D".equals(decoderFlag)) {
							String aux = matcher.group(3);
							StringTokenizer st = new StringTokenizer(aux, ",");
							while (st.hasMoreTokens()) {
								String token = st.nextToken().trim();
								if (!res.contains(token)) {
									res.add(token);
								}
							}
						}
					} else {
						break;
					}
				} else if (line.trim().equals("File formats:")) {
					evaluate = true;
				}
			}
		} catch (IOException e) {
			throw new EncoderException(e);
		} finally {
		    if(reader != null)
                try
                {
                    reader.close();
                }
                catch (IOException e)
                {
                }
			ffmpeg.destroy();
		}
		int size = res.size();
		String[] ret = new String[size];
		for (int i = 0; i < size; i++) {
			ret[i] = (String) res.get(i);
		}
		return ret;
	}

	/**
	 * Returns a set informations about a multimedia file, if its format is
	 * supported for decoding.
	 * 
	 * @param source
	 *            The source multimedia file.
	 * @return A set of informations about the file and its contents.
	 * @throws InputFormatException
	 *             If the format of the source file cannot be recognized and
	 *             decoded.
	 * @throws EncoderException
	 *             If a problem occurs calling the underlying ffmpeg executable.
	 */
	public MultimediaInfo getInfo(File source) throws InputFormatException,
			EncoderException {
		FFMPEGExecutor ffmpeg = locator.createExecutor();
		ffmpeg.addArgument("-i");
		ffmpeg.addArgument(source.getAbsolutePath());
		try {
			ffmpeg.execute();
		} catch (IOException e) {
			throw new EncoderException(e);
		}
		try {
			RBufferedReader reader = null;
			reader = new RBufferedReader(new InputStreamReader(ffmpeg
					.getErrorStream()));
			return parseMultimediaInfo(source, reader);
		} finally {
			ffmpeg.destroy();
		}
	}

	/**
	 * Private utility. It parses the ffmpeg output, extracting informations
	 * about a source multimedia file.
	 * 
	 * @param source
	 *            The source multimedia file.
	 * @param reader
	 *            The ffmpeg output channel.
	 * @return A set of informations about the source multimedia file and its
	 *         contents.
	 * @throws InputFormatException
	 *             If the format of the source file cannot be recognized and
	 *             decoded.
	 * @throws EncoderException
	 *             If a problem occurs calling the underlying ffmpeg executable.
	 */
	private MultimediaInfo parseMultimediaInfo(File source,
			RBufferedReader reader) throws InputFormatException,
			EncoderException {
		Pattern p1 = Pattern.compile("^\\s*Input #0, (\\w+).+$\\s*",
				Pattern.CASE_INSENSITIVE);
		Pattern p2 = Pattern.compile(
				"^\\s*Duration: (\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d).*$",
				Pattern.CASE_INSENSITIVE);
		Pattern p3 = Pattern.compile(
				"^\\s*Stream #\\S+: ((?:Audio)|(?:Video)|(?:Data)): (.*)\\s*$",
				Pattern.CASE_INSENSITIVE);
		MultimediaInfo info = null;
		try {
			int step = 0;
			while (true) {
				String line = reader.readLine();
				if (line == null) {
					break;
				}
				if (step == 0) {
					String token = source.getAbsolutePath() + ": ";
					if (line.startsWith(token)) {
						String message = line.substring(token.length());
						throw new InputFormatException(message);
					}
					Matcher m = p1.matcher(line);
					if (m.matches()) {
						String format = m.group(1);
						info = new MultimediaInfo();
						info.setFormat(format);
						step++;
					}
				} else if (step == 1) {
					Matcher m = p2.matcher(line);
					if (m.matches()) {
						long hours = Integer.parseInt(m.group(1));
						long minutes = Integer.parseInt(m.group(2));
						long seconds = Integer.parseInt(m.group(3));
						long dec = Integer.parseInt(m.group(4));
						long duration = (dec * 100L) + (seconds * 1000L)
								+ (minutes * 60L * 1000L)
								+ (hours * 60L * 60L * 1000L);
						info.setDuration(duration);
						step++;
					} else {
						step = 3;
					}
				} else if (step == 2) {
					Matcher m = p3.matcher(line);
					if (m.matches()) {
						String type = m.group(1);
						String specs = m.group(2);
						if ("Video".equalsIgnoreCase(type)) {
							VideoInfo video = new VideoInfo();
							StringTokenizer st = new StringTokenizer(specs, ",");
							for (int i = 0; st.hasMoreTokens(); i++) {
								String token = st.nextToken().trim();
								if (i == 0) {
									video.setDecoder(token);
								} else {
									boolean parsed = false;
									// Video size.
									Matcher m2 = SIZE_PATTERN.matcher(token);
									if (!parsed && m2.find()) {
										int width = Integer.parseInt(m2
												.group(1));
										int height = Integer.parseInt(m2
												.group(2));
										video.setSize(new VideoSize(width,
												height));
										parsed = true;
									}
									// Frame rate.
									m2 = FRAME_RATE_PATTERN.matcher(token);
									if (!parsed && m2.find()) {
										try {
											float frameRate = Float
													.parseFloat(m2.group(1));
											video.setFrameRate(frameRate);
										} catch (NumberFormatException e) {
											;
										}
										parsed = true;
									}
									// Bit rate.
									m2 = BIT_RATE_PATTERN.matcher(token);
									if (!parsed && m2.find()) {
										int bitRate = Integer.parseInt(m2
												.group(1));
										video.setBitRate(bitRate);
										parsed = true;
									}
								}
							}
							info.setVideo(video);
						} else if ("Audio".equalsIgnoreCase(type)) {
							AudioInfo audio = new AudioInfo();
							StringTokenizer st = new StringTokenizer(specs, ",");
							for (int i = 0; st.hasMoreTokens(); i++) {
								String token = st.nextToken().trim();
								if (i == 0) {
									audio.setDecoder(token);
								} else {
									boolean parsed = false;
									// Sampling rate.
									Matcher m2 = SAMPLING_RATE_PATTERN
											.matcher(token);
									if (!parsed && m2.find()) {
										int samplingRate = Integer.parseInt(m2
												.group(1));
										audio.setSamplingRate(samplingRate);
										parsed = true;
									}
									// Channels.
									m2 = CHANNELS_PATTERN.matcher(token);
									if (!parsed && m2.find()) {
										String ms = m2.group(1);
										if ("mono".equalsIgnoreCase(ms)) {
											audio.setChannels(1);
										} else if ("stereo"
												.equalsIgnoreCase(ms)) {
											audio.setChannels(2);
										}
										parsed = true;
									}
									// Bit rate.
									m2 = BIT_RATE_PATTERN.matcher(token);
									if (!parsed && m2.find()) {
										int bitRate = Integer.parseInt(m2
												.group(1));
										audio.setBitRate(bitRate);
										parsed = true;
									}
								}
							}
							info.setAudio(audio);
						}
					} else {
						step = 3;
					}
				}
				if (step == 3) {
					reader.reinsertLine(line);
					break;
				}
			}
		} catch (IOException e) {
			throw new EncoderException(e);
		}
		if (info == null) {
			throw new InputFormatException();
		}
		return info;
	}

	/**
	 * Private utility. Parse a line and try to match its contents against the
	 * {@link Encoder#PROGRESS_INFO_PATTERN} pattern. It the line can be parsed,
	 * it returns a hashtable with progress informations, otherwise it returns
	 * null.
	 * 
	 * @param line
	 *            The line from the ffmpeg output.
	 * @return A hashtable with the value reported in the line, or null if the
	 *         given line can not be parsed.
	 */
	private Hashtable<String,String> parseProgressInfoLine(String line) {
		Hashtable<String,String> table = null;
		Matcher m = PROGRESS_INFO_PATTERN.matcher(line);
		while (m.find()) {
			if (table == null) {
				table = new Hashtable<String,String>();
			}
			String key = m.group(1);
			String value = m.group(2);
			table.put(key, value);
		}
		return table;
	}

	/**
	 * Re-encode a multimedia file.
	 * 
	 * @param source
	 *            The source multimedia file. It cannot be null. Be sure this
	 *            file can be decoded (see
	 *            {@link Encoder#getSupportedDecodingFormats()},
	 *            {@link Encoder#getAudioDecoders()} and
	 *            {@link Encoder#getVideoDecoders()}).
	 * @param target
	 *            The target multimedia re-encoded file. It cannot be null. If
	 *            this file already exists, it will be overwrited.
	 * @param attributes
	 *            A set of attributes for the encoding process.
	 * @throws IllegalArgumentException
	 *             If both audio and video parameters are null.
	 * @throws InputFormatException
	 *             If the source multimedia file cannot be decoded.
	 * @throws EncoderException
	 *             If a problems occurs during the encoding process.
	 */
	public void encode(File source, File target, EncodingAttributes attributes)
			throws IllegalArgumentException, InputFormatException,
			EncoderException {
		encode(source, target, attributes, null);
	}

	/**
	 * Re-encode a multimedia file.
	 * 
	 * @param source
	 *            The source multimedia file. It cannot be null. Be sure this
	 *            file can be decoded (see
	 *            {@link Encoder#getSupportedDecodingFormats()},
	 *            {@link Encoder#getAudioDecoders()} and
	 *            {@link Encoder#getVideoDecoders()}).
	 * @param target
	 *            The target multimedia re-encoded file. It cannot be null. If
	 *            this file already exists, it will be overwrited.
	 * @param attributes
	 *            A set of attributes for the encoding process.
	 * @param listener
	 *            An optional progress listener for the encoding process. It can
	 *            be null.
	 * @throws IllegalArgumentException
	 *             If both audio and video parameters are null.
	 * @throws InputFormatException
	 *             If the source multimedia file cannot be decoded.
	 * @throws EncoderException
	 *             If a problems occurs during the encoding process.
	 *             
	 *             
	 *             [ INFO] 2015-08-12 09:07:00 [ 执行转码:-i /Users/tenney/M2Workspace/being-edu-parent/being-occup-compete/src/main/webapp/attach/compete/advert/201508/wmv-320x2401.wmv -s 720x480 -qscale 0.0 -strict -2 -f mp4 -y /Users/tenney/M2Workspace/being-edu-parent/being-occup-compete/src/main/webapp/transcoded/attach/compete/advert/201508/wmv-320x2401.mp4  ]- com.tenney.utils.jave.FFMPEGExecutor
                    [ INFO] 2015-08-12 09:07:00 [ --------------------本次用户同步任务结束--------------- ]- com.being.edu.manage.service.tasks.UserSynchronizeJob
                    [ INFO] 2015-08-12 09:07:00 [   Metadata: ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:00 [     encoder         : Lavf52.55.0 ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:00 [   Duration: 00:06:14.84, start: 0.000000, bitrate: 481 kb/s ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:00 [     Stream #0:0: Video: wmv2 (WMV2 / 0x32564D57), yuv420p, 320x240, 11.92 fps, 11.92 tbr, 1k tbn, 1k tbc ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:00 [     Stream #0:1: Audio: wmav2 (a[1][0][0] / 0x0161), 44100 Hz, 2 channels, fltp, 128 kb/s ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:00 [ Please use -q:a or -q:v, -qscale is ambiguous ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:00 [ [libx264 @ 0x7fa24380b200] using cpu capabilities: MMX2 SSE2Fast SSSE3 SSE4.2 AVX AVX2 FMA3 LZCNT BMI2 ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:00 [ [libx264 @ 0x7fa24380b200] profile High, level 2.2 ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:00 [ [libx264 @ 0x7fa24380b200] 264 - core 142 - H.264/MPEG-4 AVC codec - Copyleft 2003-2014 - http://www.videolan.org/x264.html - options: cabac=1 ref=3 deblock=1:0:0 analyse=0x3:0x113 me=hex subme=7 psy=1 psy_rd=1.00:0.00 mixed_ref=1 me_range=16 chroma_me=1 trellis=1 8x8dct=1 cqm=0 deadzone=21,11 fast_pskip=1 chroma_qp_offset=-2 threads=6 lookahead_threads=1 sliced_threads=0 nr=0 decimate=1 interlaced=0 bluray_compat=0 constrained_intra=0 bframes=3 b_pyramid=2 b_adapt=1 b_bias=0 direct=1 weightb=1 open_gop=0 weightp=2 keyint=250 keyint_min=11 scenecut=40 intra_refresh=0 rc_lookahead=40 rc=crf mbtree=1 crf=23.0 qcomp=0.60 qpmin=0 qpmax=69 qpstep=4 ip_ratio=1.40 aq=1:1.00 ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:00 [ Output #0, mp4, to '/Users/tenney/M2Workspace/being-edu-parent/being-occup-compete/src/main/webapp/transcoded/attach/compete/advert/201508/wmv-320x2401.mp4': ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:00 [   Metadata: ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:00 [     encoder         : Lavf56.36.100 ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:00 [     Stream #0:0: Video: h264 (libx264) ([33][0][0][0] / 0x0021), yuv420p, 720x480, q=-1--1, 11.92 fps, 18304 tbn, 11.92 tbc ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:00 [     Metadata: ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:00 [       encoder         : Lavc56.41.100 libx264 ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:00 [     Stream #0:1: Audio: aac ([64][0][0][0] / 0x0040), 44100 Hz, stereo, fltp, 128 kb/s ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:00 [     Metadata: ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:00 [       encoder         : Lavc56.41.100 aac ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:00 [ Stream mapping: ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:00 [   Stream #0:0 -> #0:0 (wmv2 (native) -> h264 (libx264)) ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:00 [   Stream #0:1 -> #0:1 (wmav2 (native) -> aac (native)) ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:00 [ Press [q] to stop, [?] for help ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:00 [ frame=   65 fps=0.0 q=26.0 size=     107kB time=00:00:05.34 bitrate= 164.7kbits/s     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:01 [ frame=   96 fps= 92 q=26.0 size=     248kB time=00:00:07.98 bitrate= 254.3kbits/s     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:01 [ frame=  134 fps= 86 q=26.0 size=     428kB time=00:00:11.19 bitrate= 313.2kbits/s     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:02 [ frame=  170 fps= 83 q=26.0 size=     610kB time=00:00:14.20 bitrate= 351.5kbits/s     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:02 [ frame=  200 fps= 78 q=26.0 size=     711kB time=00:00:16.76 bitrate= 347.3kbits/s     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:03 [ frame=  248 fps= 80 q=26.0 size=     863kB time=00:00:20.75 bitrate= 340.4kbits/s     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:03 [ frame=  275 fps= 77 q=26.0 size=    1023kB time=00:00:23.03 bitrate= 364.0kbits/s     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:04 [ frame=  309 fps= 75 q=26.0 size=    1207kB time=00:00:25.91 bitrate= 381.6kbits/s     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:04 [ frame=  345 fps= 75 q=26.0 size=    1564kB time=00:00:28.93 bitrate= 443.0kbits/s     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:05 [ frame=  380 fps= 74 q=26.0 size=    1854kB time=00:00:31.85 bitrate= 476.7kbits/s     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:05 [ frame=  408 fps= 73 q=26.0 size=    2122kB time=00:00:34.22 bitrate= 507.9kbits/s     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:06 [ frame=  456 fps= 74 q=26.0 size=    2396kB time=00:00:38.26 bitrate= 513.0kbits/s     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:06 [ frame=  515 fps= 78 q=26.0 size=    2736kB time=00:00:43.23 bitrate= 518.5kbits/s     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:07 [ frame=  576 fps= 81 q=26.0 size=    3015kB time=00:00:48.29 bitrate= 511.4kbits/s     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:07 [ frame=  640 fps= 84 q=26.0 size=    3308kB time=00:00:53.63 bitrate= 505.2kbits/s dup=1 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:08 [ frame=  704 fps= 86 q=26.0 size=    3615kB time=00:00:58.97 bitrate= 502.2kbits/s dup=1 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:08 [ frame=  758 fps= 88 q=26.0 size=    3932kB time=00:01:03.48 bitrate= 507.4kbits/s dup=1 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:09 [ frame=  789 fps= 86 q=26.0 size=    4249kB time=00:01:06.17 bitrate= 526.1kbits/s dup=1 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:09 [ frame=  828 fps= 86 q=26.0 size=    4545kB time=00:01:09.37 bitrate= 536.7kbits/s dup=1 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:10 [ frame=  889 fps= 88 q=26.0 size=    4811kB time=00:01:14.53 bitrate= 528.8kbits/s dup=1 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:10 [ frame=  966 fps= 91 q=26.0 size=    5119kB time=00:01:20.98 bitrate= 517.8kbits/s dup=1 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:11 [ frame= 1041 fps= 93 q=26.0 size=    5392kB time=00:01:27.30 bitrate= 505.9kbits/s dup=1 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:11 [ frame= 1118 fps= 96 q=26.0 size=    5633kB time=00:01:33.71 bitrate= 492.5kbits/s dup=1 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:12 [ frame= 1172 fps= 96 q=26.0 size=    5917kB time=00:01:38.30 bitrate= 493.1kbits/s dup=1 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:12 [ frame= 1227 fps= 97 q=26.0 size=    6043kB time=00:01:42.95 bitrate= 480.8kbits/s dup=1 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:13 [ frame= 1289 fps= 98 q=26.0 size=    6101kB time=00:01:48.15 bitrate= 462.1kbits/s dup=1 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:13 [ frame= 1350 fps= 99 q=26.0 size=    6167kB time=00:01:53.21 bitrate= 446.2kbits/s dup=1 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:14 [ frame= 1414 fps=100 q=26.0 size=    6242kB time=00:01:58.55 bitrate= 431.3kbits/s dup=1 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:14 [ frame= 1482 fps=101 q=26.0 size=    6348kB time=00:02:04.26 bitrate= 418.5kbits/s dup=1 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:15 [ frame= 1557 fps=103 q=26.0 size=    6513kB time=00:02:10.58 bitrate= 408.6kbits/s dup=1 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:15 [ frame= 1628 fps=104 q=26.0 size=    6790kB time=00:02:16.52 bitrate= 407.4kbits/s dup=1 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:16 [ frame= 1713 fps=106 q=26.0 size=    6980kB time=00:02:23.72 bitrate= 397.9kbits/s dup=1 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:16 [ frame= 1801 fps=108 q=26.0 size=    7119kB time=00:02:31.15 bitrate= 385.8kbits/s dup=1 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:17 [ frame= 1880 fps=109 q=26.0 size=    7317kB time=00:02:37.60 bitrate= 380.3kbits/s dup=2 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:17 [ frame= 1973 fps=111 q=26.0 size=    7469kB time=00:02:45.41 bitrate= 369.9kbits/s dup=2 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:18 [ frame= 2056 fps=113 q=26.0 size=    7625kB time=00:02:52.46 bitrate= 362.2kbits/s dup=2 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:19 [ frame= 2130 fps=114 q=26.0 size=    7795kB time=00:02:58.59 bitrate= 357.5kbits/s dup=2 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:19 [ frame= 2205 fps=115 q=26.0 size=    7919kB time=00:03:04.91 bitrate= 350.8kbits/s dup=2 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:20 [ frame= 2261 fps=115 q=26.0 size=    8166kB time=00:03:09.60 bitrate= 352.8kbits/s dup=2 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:20 [ frame= 2324 fps=115 q=26.0 size=    8428kB time=00:03:14.94 bitrate= 354.2kbits/s dup=2 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:21 [ frame= 2375 fps=115 q=26.0 size=    8761kB time=00:03:19.21 bitrate= 360.3kbits/s dup=2 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:21 [ frame= 2419 fps=114 q=26.0 size=    8997kB time=00:03:22.93 bitrate= 363.2kbits/s dup=2 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:22 [ frame= 2441 fps=112 q=26.0 size=    9239kB time=00:03:24.83 bitrate= 369.5kbits/s dup=2 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:22 [ frame= 2470 fps=111 q=26.0 size=    9592kB time=00:03:27.20 bitrate= 379.2kbits/s dup=2 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:23 [ frame= 2504 fps=110 q=26.0 size=    9895kB time=00:03:30.08 bitrate= 385.9kbits/s dup=2 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:23 [ frame= 2540 fps=109 q=26.0 size=   10201kB time=00:03:33.14 bitrate= 392.1kbits/s dup=2 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:24 [ frame= 2582 fps=109 q=26.0 size=   10521kB time=00:03:36.63 bitrate= 397.9kbits/s dup=2 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:24 [ frame= 2624 fps=108 q=26.0 size=   10827kB time=00:03:40.16 bitrate= 402.9kbits/s dup=2 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:25 [ frame= 2664 fps=107 q=26.0 size=   11111kB time=00:03:43.50 bitrate= 407.2kbits/s dup=2 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:25 [ frame= 2700 fps=107 q=26.0 size=   11467kB time=00:03:46.52 bitrate= 414.7kbits/s dup=2 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:26 [ frame= 2734 fps=106 q=26.0 size=   11800kB time=00:03:49.40 bitrate= 421.4kbits/s dup=2 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:26 [ frame= 2770 fps=105 q=26.0 size=   12211kB time=00:03:52.41 bitrate= 430.4kbits/s dup=2 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:27 [ frame= 2809 fps=104 q=26.0 size=   12565kB time=00:03:55.67 bitrate= 436.8kbits/s dup=2 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:27 [ frame= 2856 fps=104 q=26.0 size=   12891kB time=00:03:59.66 bitrate= 440.6kbits/s dup=2 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:28 [ frame= 2886 fps=103 q=26.0 size=   13245kB time=00:04:02.12 bitrate= 448.1kbits/s dup=2 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:28 [ frame= 2914 fps=103 q=26.0 size=   13574kB time=00:04:04.49 bitrate= 454.8kbits/s dup=2 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:29 [ frame= 2956 fps=102 q=26.0 size=   13874kB time=00:04:08.06 bitrate= 458.2kbits/s dup=2 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:29 [ frame= 2985 fps=101 q=26.0 size=   14225kB time=00:04:10.48 bitrate= 465.2kbits/s dup=2 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:30 [ frame= 3031 fps=101 q=26.0 size=   14571kB time=00:04:14.33 bitrate= 469.3kbits/s dup=2 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:30 [ frame= 3076 fps=101 q=26.0 size=   14882kB time=00:04:18.09 bitrate= 472.4kbits/s dup=2 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:31 [ frame= 3115 fps=101 q=26.0 size=   15180kB time=00:04:21.35 bitrate= 475.8kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:31 [ frame= 3144 fps=100 q=26.0 size=   15551kB time=00:04:23.67 bitrate= 483.2kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:32 [ frame= 3175 fps= 99 q=26.0 size=   15900kB time=00:04:26.31 bitrate= 489.1kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:32 [ frame= 3208 fps= 99 q=26.0 size=   16259kB time=00:04:29.10 bitrate= 494.9kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:33 [ frame= 3247 fps= 98 q=26.0 size=   16631kB time=00:04:32.40 bitrate= 500.1kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:33 [ frame= 3294 fps= 98 q=26.0 size=   16899kB time=00:04:36.34 bitrate= 500.9kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:34 [ frame= 3325 fps= 97 q=26.0 size=   17070kB time=00:04:38.95 bitrate= 501.3kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:35 [ frame= 3352 fps= 96 q=26.0 size=   17381kB time=00:04:41.22 bitrate= 506.3kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:35 [ frame= 3377 fps= 96 q=26.0 size=   17667kB time=00:04:43.31 bitrate= 510.8kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:36 [ frame= 3406 fps= 95 q=26.0 size=   18005kB time=00:04:45.72 bitrate= 516.2kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:36 [ frame= 3443 fps= 95 q=26.0 size=   18265kB time=00:04:48.88 bitrate= 517.9kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:37 [ frame= 3471 fps= 94 q=23.0 size=   18612kB time=00:04:51.20 bitrate= 523.6kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:37 [ frame= 3514 fps= 94 q=26.0 size=   18933kB time=00:04:54.83 bitrate= 526.1kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:38 [ frame= 3557 fps= 94 q=26.0 size=   19230kB time=00:04:58.40 bitrate= 527.9kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:38 [ frame= 3595 fps= 94 q=26.0 size=   19538kB time=00:05:01.61 bitrate= 530.7kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:39 [ frame= 3621 fps= 93 q=26.0 size=   19870kB time=00:05:03.84 bitrate= 535.7kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:39 [ frame= 3655 fps= 93 q=26.0 size=   20237kB time=00:05:06.62 bitrate= 540.7kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:40 [ frame= 3688 fps= 92 q=26.0 size=   20600kB time=00:05:09.41 bitrate= 545.4kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:40 [ frame= 3722 fps= 92 q=26.0 size=   20948kB time=00:05:12.29 bitrate= 549.5kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:41 [ frame= 3767 fps= 92 q=26.0 size=   21246kB time=00:05:16.05 bitrate= 550.7kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:41 [ frame= 3796 fps= 92 q=26.0 size=   21571kB time=00:05:18.51 bitrate= 554.8kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:42 [ frame= 3825 fps= 91 q=26.0 size=   21926kB time=00:05:20.92 bitrate= 559.7kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:42 [ frame= 3860 fps= 91 q=26.0 size=   22245kB time=00:05:23.85 bitrate= 562.7kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:43 [ frame= 3894 fps= 91 q=26.0 size=   22553kB time=00:05:26.73 bitrate= 565.5kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:43 [ frame= 3937 fps= 90 q=26.0 size=   22903kB time=00:05:30.35 bitrate= 567.9kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:44 [ frame= 3977 fps= 90 q=26.0 size=   23240kB time=00:05:33.69 bitrate= 570.5kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:44 [ frame= 4016 fps= 90 q=26.0 size=   23536kB time=00:05:36.95 bitrate= 572.2kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:45 [ frame= 4048 fps= 90 q=26.0 size=   23874kB time=00:05:39.64 bitrate= 575.8kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:45 [ frame= 4080 fps= 89 q=26.0 size=   24237kB time=00:05:42.33 bitrate= 580.0kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:46 [ frame= 4112 fps= 89 q=26.0 size=   24595kB time=00:05:45.03 bitrate= 584.0kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:46 [ frame= 4152 fps= 89 q=26.0 size=   25011kB time=00:05:48.37 bitrate= 588.1kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:47 [ frame= 4196 fps= 89 q=26.0 size=   25368kB time=00:05:52.08 bitrate= 590.2kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:47 [ frame= 4257 fps= 89 q=26.0 size=   25631kB time=00:05:57.19 bitrate= 587.8kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:48 [ frame= 4298 fps= 89 q=26.0 size=   26015kB time=00:06:00.63 bitrate= 590.9kbits/s dup=3 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:48 [ frame= 4346 fps= 89 q=26.0 size=   26420kB time=00:06:04.58 bitrate= 593.6kbits/s dup=4 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:49 [ frame= 4432 fps= 90 q=26.0 size=   26606kB time=00:06:11.87 bitrate= 586.1kbits/s dup=4 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:49 [ frame= 4461 fps= 90 q=-1.0 Lsize=   27048kB time=00:06:14.84 bitrate= 591.1kbits/s dup=4 drop=0     ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:49 [ video:21518kB audio:5336kB subtitle:0kB other streams:0kB global headers:0kB muxing overhead: 0.721018% ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:49 [ [libx264 @ 0x7fa24380b200] frame I:34    Avg QP:15.04  size: 38728 ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:49 [ [libx264 @ 0x7fa24380b200] frame P:1927  Avg QP:19.88  size:  8866 ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:49 [ [libx264 @ 0x7fa24380b200] frame B:2500  Avg QP:23.14  size:  1453 ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:49 [ [libx264 @ 0x7fa24380b200] consecutive B-frames: 19.2% 16.5%  5.6% 58.7% ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:49 [ [libx264 @ 0x7fa24380b200] mb I  I16..4: 21.2% 60.6% 18.2% ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:49 [ [libx264 @ 0x7fa24380b200] mb P  I16..4:  3.6% 12.5%  1.2%  P16..4: 26.7% 10.3%  4.3%  0.0%  0.0%    skip:41.5% ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:49 [ [libx264 @ 0x7fa24380b200] mb B  I16..4:  0.1%  0.4%  0.1%  B16..8: 17.2%  2.1%  0.4%  direct: 2.7%  skip:77.1%  L0:41.6% L1:49.4% BI: 9.0% ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:49 [ [libx264 @ 0x7fa24380b200] 8x8 transform intra:71.0% inter:77.9% ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:49 [ [libx264 @ 0x7fa24380b200] coded y,uvDC,uvAC intra: 50.5% 72.8% 58.3% inter: 11.8% 20.2% 7.9% ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:49 [ [libx264 @ 0x7fa24380b200] i16 v,h,dc,p: 32% 53% 10%  6% ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:49 [ [libx264 @ 0x7fa24380b200] i8 v,h,dc,ddl,ddr,vr,hd,vl,hu: 22% 26% 22%  4%  5%  5%  6%  4%  7% ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:49 [ [libx264 @ 0x7fa24380b200] i4 v,h,dc,ddl,ddr,vr,hd,vl,hu: 26% 30% 12%  4%  7%  5%  6%  4%  5% ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:49 [ [libx264 @ 0x7fa24380b200] i8c dc,h,v,p: 37% 39% 16%  8% ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:49 [ [libx264 @ 0x7fa24380b200] Weighted P-Frames: Y:1.6% UV:1.6% ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:49 [ [libx264 @ 0x7fa24380b200] ref P L0: 70.8% 13.7% 10.7%  4.7%  0.0% ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:49 [ [libx264 @ 0x7fa24380b200] ref B L0: 87.2% 11.2%  1.6% ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:49 [ [libx264 @ 0x7fa24380b200] ref B L1: 95.5%  4.5% ]- com.tenney.utils.jave.Encoder
                    [ INFO] 2015-08-12 09:07:49 [ [libx264 @ 0x7fa24380b200] kb/s:470.87 ]- com.tenney.utils.jave.Encoder
	 *             
	 *             
	 */
	public void encode(File source, File target, EncodingAttributes attributes,
			EncoderProgressListener listener) throws IllegalArgumentException,
			InputFormatException, EncoderException {
		String formatAttribute = attributes.getFormat();
		Float offsetAttribute = attributes.getOffset();
		Float durationAttribute = attributes.getDuration();
		AudioAttributes audioAttributes = attributes.getAudioAttributes();
		VideoAttributes videoAttributes = attributes.getVideoAttributes();
		if (audioAttributes == null && videoAttributes == null) {
			throw new IllegalArgumentException(
					"Both audio and video attributes are null");
		}
		target = target.getAbsoluteFile();
		target.getParentFile().mkdirs();
		FFMPEGExecutor ffmpeg = locator.createExecutor();
		if (offsetAttribute != null) {
			ffmpeg.addArgument("-ss");
			ffmpeg.addArgument(String.valueOf(offsetAttribute.floatValue()));
		}
		ffmpeg.addArgument("-i");
		ffmpeg.addArgument(source.getAbsolutePath());
		if (durationAttribute != null) {
			ffmpeg.addArgument("-t");
			ffmpeg.addArgument(String.valueOf(durationAttribute.floatValue()));
		}
		if (videoAttributes == null) {
			ffmpeg.addArgument("-vn");
		} else {
			String codec = videoAttributes.getCodec();
			if (codec != null) {
				ffmpeg.addArgument("-vcodec");
				ffmpeg.addArgument(codec);
			}
			String tag = videoAttributes.getTag();
			if (tag != null) {
				ffmpeg.addArgument("-vtag");
				ffmpeg.addArgument(tag);
			}
			Integer bitRate = videoAttributes.getBitRate();
			if (bitRate != null) {
				ffmpeg.addArgument("-b");
				ffmpeg.addArgument(String.valueOf(bitRate.intValue()));
			}
			Integer frameRate = videoAttributes.getFrameRate();
			if (frameRate != null) {
				ffmpeg.addArgument("-r");
				ffmpeg.addArgument(String.valueOf(frameRate.intValue()));
			}
			VideoSize size = videoAttributes.getSize();
			if (size != null) {
				ffmpeg.addArgument("-s");
				ffmpeg.addArgument(String.valueOf(size.getWidth()) + "x"
						+ String.valueOf(size.getHeight()));
			}
		}
		if (audioAttributes == null) {
			ffmpeg.addArgument("-an");
		} else {
			String codec = audioAttributes.getCodec();
			if (codec != null) {
				ffmpeg.addArgument("-acodec");
				ffmpeg.addArgument(codec);
			}
			Integer bitRate = audioAttributes.getBitRate();
			if (bitRate != null) {
				ffmpeg.addArgument("-ab");
				ffmpeg.addArgument(String.valueOf(bitRate.intValue()));
			}
			Integer channels = audioAttributes.getChannels();
			if (channels != null) {
				ffmpeg.addArgument("-ac");
				ffmpeg.addArgument(String.valueOf(channels.intValue()));
			}
			Integer samplingRate = audioAttributes.getSamplingRate();
			if (samplingRate != null) {
				ffmpeg.addArgument("-ar");
				ffmpeg.addArgument(String.valueOf(samplingRate.intValue()));
			}
			Integer volume = audioAttributes.getVolume();
			if (volume != null) {
				ffmpeg.addArgument("-vol");
				ffmpeg.addArgument(String.valueOf(volume.intValue()));
			}
		}
		if(attributes.getQscale() != null){
		    ffmpeg.addArgument("-qscale");
		    ffmpeg.addArgument(attributes.getQscale());
		}
		if(attributes.getStrict() != null){
		    ffmpeg.addArgument("-strict");
		    ffmpeg.addArgument(attributes.getStrict());
		}
		ffmpeg.addArgument("-f");
		ffmpeg.addArgument(formatAttribute);
		ffmpeg.addArgument("-y");
		ffmpeg.addArgument(target.getAbsolutePath());
		try {
			ffmpeg.execute();
		} catch (IOException e) {
			throw new EncoderException(e);
		}
		try {
			String lastWarning = null;
			long duration;
			long progress = 0;
			RBufferedReader reader = null;
			reader = new RBufferedReader(new InputStreamReader(ffmpeg
					.getErrorStream()));
			MultimediaInfo info = parseMultimediaInfo(source, reader);
			if (durationAttribute != null) {
				duration = (long) Math
						.round((durationAttribute.floatValue() * 1000L));
			} else {
				duration = info.getDuration();
				if (offsetAttribute != null) {
					duration -= (long) Math
							.round((offsetAttribute.floatValue() * 1000L));
				}
			}
			if (listener != null) {
				listener.sourceInfo(info);
			}
			int step = 0;
			String line;
			while ((line = reader.readLine()) != null) {
			    logger.info(line);
				if (step == 0) {
					if (line.startsWith("WARNING: ") || line.startsWith("Please") || line.startsWith("[libx264")) {
						if (listener != null) {
							listener.message(line);
						}
					} else if (!line.startsWith("Output #0") && !line.startsWith("  Metadata:") && !line.startsWith("  Duration:") && !line.startsWith("    ")) {
						throw new EncoderException(line);
					} else if(line.startsWith("Output #0")) {
						step++;
					}
				} else if (step == 1) {
					if (!line.startsWith("  ")) {
						step++;
					}
				}
				if (step == 2) {
					if (!line.startsWith("Stream mapping:")) {
						throw new EncoderException(line);
					} else {
						step++;
					}
				} else if (step == 3) {
					if (!line.startsWith("  ")) {
						step++;
					}
				}
				if (step == 4) {
					line = line.trim();
					if (line.length() > 0) {
						Hashtable<String,String> table = parseProgressInfoLine(line);
						if (table == null) {
							if (listener != null) {
								listener.message(line);
							}
							lastWarning = line;
							if (SUCCESS_PATTERN.matcher(lastWarning).matches()) {
							    step++; //如果已经显示成功信息，直接跳出程序，后续信息不再处理
							}
						} else {
							if (listener != null) {
								String time = (String) table.get("time");
								if (time != null) {
									int dot = time.indexOf('.');
									if (dot > 0 && dot == time.length() - 2
											&& duration > 0) {
										String p1 = time.substring(0, dot);
										String p2 = time.substring(dot + 1);
										try {
											long i1 = Long.parseLong(p1);
											long i2 = Long.parseLong(p2);
											progress = (i1 * 1000L)
													+ (i2 * 100L);
											int perm = (int) Math
													.round((double) (progress * 1000L)
															/ (double) duration);
											if (perm > 1000) {
												perm = 1000;
											}
											listener.progress(perm);
										} catch (NumberFormatException e) {
											;
										}
									}
								}
							}
							lastWarning = null;
						}
					}
				}
			}
			if (lastWarning != null) {
				if (!SUCCESS_PATTERN.matcher(lastWarning).matches() && !lastWarning.startsWith("[libx264")) {
					throw new EncoderException(lastWarning);
				}
			}
		} catch (IOException e) {
			throw new EncoderException(e);
		} finally {
			ffmpeg.destroy();
		}
	}

	public void getImage(File src,File desc,float ss) throws EncoderException, FileNotFoundException{
		if(!src.exists()){
			throw new FileNotFoundException();
		}
		desc=desc.getAbsoluteFile();
		desc.getParentFile().mkdirs();
		FFMPEGExecutor ffmpeg = locator.createExecutor();
		ffmpeg.addArgument("-i");
		ffmpeg.addArgument(src.getAbsolutePath());
		ffmpeg.addArgument("-y");
		ffmpeg.addArgument("-f");
		ffmpeg.addArgument("image2");
		ffmpeg.addArgument("-ss");
		ffmpeg.addArgument(String.valueOf((int)ss));
		ffmpeg.addArgument("-t");
		ffmpeg.addArgument("0.001");
		ffmpeg.addArgument(desc.getAbsolutePath());
		
		RBufferedReader reader = null;
		try {
			ffmpeg.execute();
			reader = new RBufferedReader(new InputStreamReader(ffmpeg
					.getErrorStream()));
			String line=null;
			while((line=reader.readLine())!=null){
				logger.info(line);
			}
		} catch (IOException e) {
			throw new EncoderException(e);
		} finally {
		    if(reader != null)
                try
                {
                    reader.close();
                }
                catch (IOException e)
                {
                }
			ffmpeg.destroy();
		}
	}
	
	public static void main(String[] args){
	    Encoder encoder = new Encoder();
        String parentPath="/Users/tenney/Desktop" ;//Test.class.getResource("/").getPath();
        try {
            encoder.getImage(new File(parentPath+"/rmvb01.mp4"), new File(parentPath+"/rmvb02.jpg"), 1);
        } catch (EncoderException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } 
	}
}
