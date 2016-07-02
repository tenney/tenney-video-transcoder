# tenney-video-transcoder
a transcoder with ffmpeg for video
一个基于ffmpeg转换工具的java封装，支持视频截图、转码等,支持H264编码
目前支持winodw/linux/mac ，并在windows7、8(64)/mac 10.10.4/centos 7(64)上面测试通过， 若需要其他版本请自行编译相应的ffmpeg程序 ，替换掉 src/main/resource/下面相应执行程序 ，并修改对应encode类中相关输出判断逻辑。

具体使用请见测试代码，因ffmpeg程序版本较多，参数也各异，本程序未做到所有参数的封装，若有部分参数没有，请自行修改添加、也欢迎大家fork一起完善。

本源码借鉴了网上许多网友的想法、部分源码等，在此感谢，因当时收集代码时未曾记下来源，在此无法提供源作者，请见谅。
