#MediaUtil - A Java API Package for Media Related Utilities

Version 1.0 Updated October 1 2005
MediaUtil is an offshoot of the MediaChest Project to provide a well packaged Java API for Media Related Utilities.

Below is the Current API Offering from MediaUtil.

LLJTran

LLJTran is an API for performing Lossless Transformations on JPEG image files which also provides the Capability of handling Exif information. Following are the key features:
Supports lossless rotation, transpose, transverse and crop
Trimming or relocating Non Transformable edge blocks similiar to jpegtran or processing them like regular MCU blocks.
Reading and Modifying Image Header Information (Exif) including Thumbnail
Built-in transformation of Thumbnail and Orientation marker
Supports the IterativeReader and IterativeWriter interfaces in MediaUtil's mediautil.gen.directio package enabling things like Sharing the jpeg input file with say jkd's ImageReader while reading
Does not Support Multi-Threading for the same Object to be used simultaneously by more than one thread. However different threads can have their own LLJTran Objects.
Requires JDK 1.5
Documentation

To use Download mediautil-1.0.zip and extract it to a suitable folder. Then include the mediautil-1.0.jar file in your CLASSPATH. The download also includes source under the src directory and documentation including javadocs under the docs directory.
For Getting Started please see LLJTranTutorial.java which is a tutorial with different usage examples.

For API reference please see the Javadocs.

Projects using the MediaUtil API

Please inform if your project/product is using MediaUtil so that we can add it to the below list:
MediaChest
Contact

Dmitriy Rogatkin (metricstream@gmail.com)
Suresh Mahalingam (msuresh@cheerful.com)

License

MediaUtil is free to download, use, modify and redistribute for non-commercial and and commercial purposes without any warranties of course.
Other Projects of Dmitriy

Mediachest

Mini Java Web Server with Java container 2.3 API support

xBox - Bean box supporting XML serialization (do not confuse with proposed later java.beans.Encoder)

jAddressBook is an address book with float XML format of addresses and another profile information

Other Projects of Suresh

Jdatestamp: A Lossless Date Stamper for Digital Pictures

------
#说点自己的话
###由于项目需要，想要添加一些属性到照片里，也就是修改图片的Exif信息，自己找了好久，找到了不少，但都是用来读取属性的而不能修改属性，最后终于找到这个`MediaUtil`说能修改属性。可是jar包是下载到了，文档却找不到，主页也访问不了，最后只能硬生生的看反编译的源码，看的很费劲。忍不了了，就想办法把源码从 http://sourceforge.net 上搬到了github上以方便大家使用