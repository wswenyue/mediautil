/* MediaUtil LLJTran - $RCSfile: LLJTranTester.java,v $
 * Copyright (C) 1999-2005 Dmitriy Rogatkin, Suresh Mahalingam.  All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  $Id: LLJTranTester.java,v 1.4 2005/08/18 04:35:34 drogatkin Exp $
 *
 * Some ideas and algorithms were borrowed from:
 * Thomas G. Lane, and James R. Weeks
 */
package mediautil.test;

import java.io.*;
import java.util.Date;
import java.awt.*;
import java.awt.image.*;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;

import mediautil.gen.directio.*;
import mediautil.image.ImageResources;
import mediautil.image.jpeg.LLJTran;
import mediautil.image.jpeg.Exif;
import mediautil.image.jpeg.Entry;
import mediautil.image.jpeg.LLJTranException;

public class LLJTranTester {

    public static void javaApi(InputStream ip, OutputStream op, OutputStream op1)
        throws IOException
    {
        ImageReader reader;
        ImageInputStream iis = ImageIO.createImageInputStream(ip);
        reader = (ImageReader) ImageIO.getImageReaders(iis).next();
        reader.setInput(iis);
        System.out.println("Reader = " + reader);
System.out.println("********** BEGIN READ: " + new Date());
        BufferedImage image = reader.read(0);
System.out.println("********** END READ: " + new Date());
        Graphics graphics = image.getGraphics();
        graphics.setColor(new Color(240, 100, 255, 255));
        graphics.drawString("Highlight " + new Date(), 20, image.getHeight() - 40);
        // Save modified image
        String format = "JPG";
        ImageIO.write(image, format, op);
        if(op1 != null)
            ImageIO.write(image, format, op1);
        iis.close();
    }

    public static LLJTran readShared(String fileName, boolean keep_appxs)
        throws LLJTranException, IOException
    {
        FileInputStream fip = new FileInputStream(fileName);
        SplitInputStream sip = new SplitInputStream(fip);
        InputStream subIp = sip.createSubStream();
        LLJTran llj = new LLJTran(subIp);
        llj.initRead(LLJTran.READ_ALL, keep_appxs, true);
        sip.attachSubReader(llj, subIp);

        // Read Image and load LLJTran
        BufferedOutputStream fop = new BufferedOutputStream(new FileOutputStream("d.jpg"));
        javaApi(sip, fop, null);
System.out.println("Max Blocks = " + sip.getMaxBufSize());
System.out.println(" minRead = " + llj.getRequestSize(0) + " maxRead = " + llj.getRequestSize(1));

        sip.wrapup();
        fip.close();
        fop.close();

        System.out.println("frm_x = " + llj.getWidth() + " frm_y = " + llj.getHeight()
                        + " maxHi = " + llj.getMaxHSamplingFactor() + " maxVi = " + llj.getMaxVSamplingFactor()
                        + " widthMCU = " + llj.getWidthInMCU() + " heightMCU = " + llj.getHeightInMCU());
        System.out.println("Info = " + llj.getImageInfo());
        System.out.println("Successfully Read Image");

        return llj;
    }

    public static void readImage(LLJTran llj, boolean keep_appxs, int stage1,
                            int stage2, int stage3)
        throws LLJTranException
    {
        if(stage1 != 0)
            llj.read(stage1, keep_appxs);
        if(stage2 != 0)
            llj.read(stage2, keep_appxs);
        System.out.println("frm_x = " + llj.getWidth() + " frm_y = " + llj.getHeight()
                        + " maxHi = " + llj.getMaxHSamplingFactor() + " maxVi = " + llj.getMaxVSamplingFactor()
                        + " widthMCU = " + llj.getWidthInMCU() + " heightMCU = " + llj.getHeightInMCU());
        if(stage3 != 0)
            llj.read(stage3, keep_appxs);
        System.out.println("Info = " + llj.getImageInfo());
        System.out.println("Successfully Read Image");
    }

    public static void main1(String[] args) throws Exception {
        LLJTran llj = new LLJTran(new File(args[0]));
        readImage(llj, true, LLJTran.READ_ALL, 0, 0);
        Rectangle cropArea = new Rectangle();
        byte newThumbnail[] = new byte[100000];
        int l;
        if(llj.getImageInfo().getThumbnailLength() > 0)
        {
            FileOutputStream top = new FileOutputStream("del.jpg");
            InputStream tip = llj.getThumbnailAsStream();
            while ((l=tip.read(newThumbnail)) > 0)
                top.write(newThumbnail, 0, l);
            top.close();
        }
        else
            System.out.println("Image has no Thumbnail");
        if(llj.getImageInfoAppxIndex() < 0)
        {
            System.out.println("Attempting to add Dummy Exif Header");
            llj.addAppx(LLJTran.dummyExifHeader, 0,
                        LLJTran.dummyExifHeader.length, true);
        }
        FileInputStream fip = new FileInputStream("x1.jpg");
        l = fip.read(newThumbnail); fip.close();
        llj.setThumbnail(newThumbnail, 0, l,
                             ImageResources.EXT_JPG);
        int i;
        String currentOpName, opName = args[1], prefix = null, suffix = null;
        if(args.length > 3)
        {
            i = opName.lastIndexOf('.');
            if(i < 0)
                i = opName.length();
            prefix = opName.substring(0, i) + '_';
            suffix = opName.substring(i);
        }

        currentOpName = opName;
        for(i=2; i < args.length; ++i)
        {
            int options = LLJTran.OPT_DEFAULTS | LLJTran.OPT_XFORM_ORIENTATION | LLJTran.OPT_XFORM_TRIM;
            int transformOp = Integer.parseInt(args[i]);
System.out.println("Transform begun at " + new Date());
            if(transformOp == LLJTran.CROP)
            {
                cropArea.x = Integer.parseInt(args[++i]);
                cropArea.y = Integer.parseInt(args[++i]);
                cropArea.width = Integer.parseInt(args[++i]);
                cropArea.height = Integer.parseInt(args[++i]);
                llj.transform(transformOp, options, cropArea);
            }
            else
                llj.transform(transformOp, options);
System.out.println("Transform ends at " + new Date());
            FileOutputStream op = new FileOutputStream(currentOpName);
            llj.save(op, LLJTran.OPT_WRITE_ALL);
            op.close();
            currentOpName = prefix + (i-1) + suffix;
        }
    }

    public static void main2(String[] args) throws Exception {
        /*
        LLJTran llj = new LLJTran(new File(args[0]));
        readImage(llj, true, LLJTran.READ_ALL,0, 0);
        */
System.out.println("********** BEGIN READ: " + new Date());
        LLJTran llj = readShared(args[0], true);
System.out.println("********** END READ: " + new Date());
        Rectangle cropArea = new Rectangle();
        byte newThumbnail[] = new byte[15000];
        int l;
        if(llj.getImageInfo().getThumbnailLength() > 0)
        {
            FileOutputStream top = new FileOutputStream("del.jpg");
            llj.writeThumbnail(top);
        }
        else
            System.out.println("Image has no Thumbnail");
        int i;
        String currentOpName, opName = args[1], prefix = null, suffix = null;
        if(args.length > 2)
        {
            i = opName.lastIndexOf('.');
            if(i < 0)
                i = opName.length();
            prefix = opName.substring(0, i) + '_';
            suffix = opName.substring(i);
        }

        currentOpName = opName;
        for(i=2; i < args.length; ++i)
        {
            int options = LLJTran.OPT_DEFAULTS | LLJTran.OPT_XFORM_THUMBNAIL | LLJTran.OPT_XFORM_ORIENTATION;
            int transformOp = Integer.parseInt(args[i]);
            FileOutputStream op = new FileOutputStream(currentOpName);
            if(transformOp == LLJTran.CROP)
            {
                cropArea.x = Integer.parseInt(args[++i]);
                cropArea.y = Integer.parseInt(args[++i]);
                cropArea.width = Integer.parseInt(args[++i]);
                cropArea.height = Integer.parseInt(args[++i]);
                llj.transform(op, transformOp, options, cropArea);
            }
            else
                llj.transform(op, transformOp, options);
            op.close();
            currentOpName = prefix + (i-1) + suffix;
        }
        FileOutputStream op = new FileOutputStream(currentOpName);
        llj.save(op, LLJTran.OPT_WRITE_ALL);
        op.close();
    }

    public static void main(String[] args) throws Exception {
        LLJTran llj = new LLJTran(new File(args[0]));
System.out.println("********** BEGIN READ: " + new Date());
        readImage(llj, true, LLJTran.READ_ALL, 0, 0);
System.out.println("********** END READ: " + new Date());
        Rectangle cropArea = new Rectangle();
        byte newThumbnail[] = new byte[100000];
        int l;
        if(llj.getImageInfo().getThumbnailLength() > 0)
        {
            FileOutputStream top = new FileOutputStream("del.jpg");
            InputStream tip = llj.getThumbnailAsStream();
            while ((l=tip.read(newThumbnail)) > 0)
                top.write(newThumbnail, 0, l);
            top.close();
        }
        else
            System.out.println("Image has no Thumbnail");
        if(llj.getImageInfoAppxIndex() < 0)
        {
            System.out.println("Attempting to add Dummy Exif Header");
            llj.addAppx(LLJTran.dummyExifHeader, 0,
                        LLJTran.dummyExifHeader.length, true);
        }
        FileInputStream fip = new FileInputStream("x1.jpg");
        l = fip.read(newThumbnail); fip.close();
        llj.setThumbnail(newThumbnail, 0, l,
                             ImageResources.EXT_JPG);
        int i;
        String currentOpName, opName = args[1], prefix = null, suffix = null;
        if(args.length > 3)
        {
            i = opName.lastIndexOf('.');
            if(i < 0)
                i = opName.length();
            prefix = opName.substring(0, i) + '_';
            suffix = opName.substring(i);
        }

        currentOpName = opName;
        for(i=2; i < args.length; ++i)
        {
            int options = LLJTran.OPT_DEFAULTS | LLJTran.OPT_XFORM_ORIENTATION | LLJTran.OPT_XFORM_TRIM;
            int transformOp = Integer.parseInt(args[i]);
System.out.println("Transform begun at " + new Date());
            if(transformOp == LLJTran.CROP)
            {
                cropArea.x = Integer.parseInt(args[++i]);
                cropArea.y = Integer.parseInt(args[++i]);
                cropArea.width = Integer.parseInt(args[++i]);
                cropArea.height = Integer.parseInt(args[++i]);
                llj.transform(transformOp, options, cropArea);
            }
            else
                llj.transform(transformOp, options);
System.out.println("Transform ends at " + new Date());

            InStreamFromIterativeWriter iwip = new InStreamFromIterativeWriter();

            FileOutputStream op = new FileOutputStream("d.jpg");
            IterativeWriter iWriter = llj.initWrite(iwip.getWriterOutputStream(),
                                        LLJTran.NONE, LLJTran.OPT_WRITE_ALL, null, 0, true);
            iwip.setIterativeWriter(iWriter);
            LLJTran llj1 = new LLJTran(iwip);
System.out.println("********** BEGIN READ: " + new Date());
            readImage(llj1, true, LLJTran.READ_ALL, 0, 0);
System.out.println("********** END READ: " + new Date() + " minWrite = " + llj.getRequestSize(2) + " maxWrite = " + llj.getRequestSize(3));
            llj1.transform(LLJTran.ROT_90, LLJTran.OPT_DEFAULTS);
System.out.println("Max Buf = " + iwip.getMaxBufSize());
            llj1.save(op, LLJTran.OPT_WRITE_ALL);
System.out.println("********** END SAVE: " + new Date());
            llj1.freeMemory();
            // javaApi(iwip, op);
            llj.wrapupIterativeWrite(iWriter);
            op.close();

            op = new FileOutputStream(currentOpName);
            llj.save(op, LLJTran.OPT_WRITE_ALL);
            op.close();
            currentOpName = prefix + (i-1) + suffix;
        }
    }

    public static void main4(String[] args) throws Exception {
        BufferedInputStream imageIp = new BufferedInputStream(new FileInputStream(args[0]));
        BufferedOutputStream imageOp = new BufferedOutputStream(new FileOutputStream("java.jpg"));
        OutStreamToIterativeReader opToLlj = new OutStreamToIterativeReader();
        LLJTran llj = new LLJTran(opToLlj.getReaderInputStream());
        opToLlj.setIterativeReader(llj);
        llj.initRead(LLJTran.READ_ALL, true, true);
        javaApi(imageIp, opToLlj, imageOp);
        imageIp.close();
        opToLlj.close();
        imageOp.close();
        Rectangle cropArea = new Rectangle();
        byte newThumbnail[] = new byte[15000];
        int l;
        if(llj.getImageInfo().getThumbnailLength() > 0)
        {
            FileOutputStream top = new FileOutputStream("del.jpg");
            llj.writeThumbnail(top);
        }
        else
            System.out.println("Image has no Thumbnail");
        int i;
        String currentOpName, opName = args[1], prefix = null, suffix = null;
        if(args.length > 2)
        {
            i = opName.lastIndexOf('.');
            if(i < 0)
                i = opName.length();
            prefix = opName.substring(0, i) + '_';
            suffix = opName.substring(i);
        }

        currentOpName = opName;
        for(i=2; i < args.length; ++i)
        {
            int options = LLJTran.OPT_DEFAULTS | LLJTran.OPT_XFORM_THUMBNAIL | LLJTran.OPT_XFORM_ORIENTATION;
            int transformOp = Integer.parseInt(args[i]);
            FileOutputStream op = new FileOutputStream(currentOpName);
            if(transformOp == LLJTran.CROP)
            {
                cropArea.x = Integer.parseInt(args[++i]);
                cropArea.y = Integer.parseInt(args[++i]);
                cropArea.width = Integer.parseInt(args[++i]);
                cropArea.height = Integer.parseInt(args[++i]);
                llj.transform(op, transformOp, options, cropArea);
            }
            else
                llj.transform(op, transformOp, options);
            op.close();
            currentOpName = prefix + (i-1) + suffix;
        }
        FileOutputStream op = new FileOutputStream(currentOpName);
        llj.save(op, LLJTran.OPT_WRITE_ALL);
        op.close();
    }

    public static void main5(String[] args) throws Exception {
        LLJTran llj = new LLJTran(new File(args[0]));
        readImage(llj, true, LLJTran.READ_INFO, 0, 0);
        // llj.closeInternalInputStream();
        String newTime = "";
        Exif exif = (Exif) llj.getImageInfo();

        Entry entry = exif.getTagValue(Exif.DATETIME, true);
        if(entry != null)
            entry.setValue(0, "1998:08:18 11:15:00");
        entry = exif.getTagValue(Exif.DATETIMEORIGINAL, true);
        if(entry != null)
            entry.setValue(0, "1998:08:18 11:15:00");
        entry = exif.getTagValue(Exif.DATETIMEDIGITIZED, true);
        if(entry != null)
            entry.setValue(0, "1998:08:18 11:15:00");
        entry = exif.getTagValue(Exif.ORIENTATION, true);
        System.out.println("Orient Entry = " + entry);
        if(entry != null)
            entry.setValue(0, new Integer(6));

        llj.refreshAppx();

        // FileInputStream fip = new FileInputStream("Img_1217.jpg");
        FileOutputStream nhOp = new FileOutputStream("del.jpg");
        llj.xferInfo(null, nhOp, LLJTran.REPLACE, LLJTran.REPLACE);
        // fip.close();
        nhOp.close();
        llj.read(LLJTran.READ_ALL, true);
        FileOutputStream op = new FileOutputStream(args[1]);
        int transformOp = Integer.parseInt(args[2]);
        llj.transform(op, transformOp);
        op.close();
    }
}
