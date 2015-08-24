/* MediaUtil LLJTran - $RCSfile: InStreamFromIterativeWriterTester.java,v $
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
 *  $Id: InStreamFromIterativeWriterTester.java,v 1.4 2005/08/18 04:35:34 drogatkin Exp $
 *
 */
package mediautil.test;

import java.io.*;
import java.util.Random;

import mediautil.gen.directio.IterativeReader;
import mediautil.gen.directio.IterativeWriter;
import mediautil.gen.directio.InStreamFromIterativeWriter;

class TestWriterCode implements IterativeWriter
{
    public static byte fileArr[];
    public static Random r;
    public static int id;
    private OutputStream op;
    private int pos, maxBuf, maxWrites;
    private PrintStream refOp;
    private byte writeBuf[];
    public static boolean flush = false;

    public static void printBytes(PrintStream op, byte b[], int off, int len)
    {
        while(len > 0)
        {
            op.println("" + b[off]);
            off++;
            len--;
        }
    }

    public TestWriterCode(OutputStream op, int maxBuf,
                    int maxWrites) throws FileNotFoundException
    {
        this.op = op;
        this.maxBuf = maxBuf;
        this.maxWrites = maxWrites;
        pos = 0;
        refOp = new PrintStream(new BufferedOutputStream(new FileOutputStream("D:/work/deldir/sub" + "_ref.txt"), 100000));
    }

public static boolean debug = false;

    private boolean prevShort = false;
    public int nextWrite(int numBytes)
    {
        int writeCount = r.nextInt(maxWrites) + 1;
        int len = 0, ofs, actualRead, maxLen;
        int retVal = IterativeReader.CONTINUE;
      try {
        do
        {
            len = r.nextInt(maxBuf) + 1;
            actualRead = len;
            maxLen = fileArr.length - pos;
            if(len > maxLen)
                actualRead = maxLen;
            String opMsg = "Read ";
            refOp.println("Trying Id: " + id + ' ' + opMsg + pos + " - " + (pos+len));
            if(flush)
            {
                refOp.flush();
            }

            op.write(fileArr, pos, actualRead);
            printBytes(refOp, TestWriterCode.fileArr, pos, actualRead);
            if(maxLen <= len)
                op.close();
            refOp.println("Id: " + id + " writeCount: " + writeCount + ' ' + opMsg + pos + " - " + (pos+actualRead));

            if(flush)
            {
                refOp.flush();
            }

            if(actualRead > 0)
                pos += actualRead;
            --writeCount;
        } while(maxLen > len && writeCount > 0);
        if(maxLen <= len)
        {
            retVal = IterativeReader.STOP;
            refOp.close();
        }
      } catch(Exception e)
      {
        e.printStackTrace(System.err);
        refOp.println("Exception for Id: " + id + " writeCount: " + writeCount + ' ' + " Len = " + len);
        e.printStackTrace(refOp);
        refOp.flush();
        throw new RuntimeException("What the Heaven");
      }
        return retVal;
    }

    public void closeFiles()
    {
        refOp.close();
        refOp = null;
    }
}

public class InStreamFromIterativeWriterTester extends InStreamFromIterativeWriter {

    public static void main(String args[]) throws Exception
    {
        File finfo = new File(args[0]);
        int size = (int)finfo.length();
        FileInputStream fip = new FileInputStream(finfo);
        TestWriterCode.fileArr = new byte[size];
        int readLen = fip.read(TestWriterCode.fileArr);
        int i;
        fip.close();
        int skipProb = 5, maxBuf = 200;
        InStreamFromIterativeWriter sip = new InStreamFromIterativeWriter(32, 7, 8, 8);
        TestWriterCode.r = new Random(555);
        TestWriterCode tc = new TestWriterCode(sip.getWriterOutputStream(), maxBuf, 3);
        sip.setIterativeWriter(tc);
        PrintStream refOp = new PrintStream(new BufferedOutputStream(new FileOutputStream("D:/work/deldir/main_ref.txt"), 100000));
        PrintStream sipOp = new PrintStream(new BufferedOutputStream(new FileOutputStream("D:/work/deldir/main_sip.txt"), 100000));
        boolean prevShort = false;
        int pos, len, ofs, actualRead;
        byte readBuf[] = new byte[maxBuf];
        pos = 0;
        i = 0;
        do
        {
            len = TestWriterCode.r.nextInt(maxBuf) + 1;
            boolean isSkip = TestWriterCode.r.nextInt(16) < skipProb;
            String opMsg = isSkip?"Skipped ":"Read ";
            TestWriterCode.id = i;
            refOp.println("Trying Id: " + i + ' ' + opMsg + pos + " - " + (pos+len));
            sipOp.println("Trying Id: " + i + ' ' + opMsg + pos + " - " + (pos+len));
            if(TestWriterCode.flush)
            {
                refOp.flush();
                sipOp.flush();
            }
            if(isSkip)
                actualRead = (int)sip.skip(len);
            else {
                ofs = TestWriterCode.r.nextInt(maxBuf-len+1);
                actualRead = sip.read(readBuf, ofs, len);
                TestWriterCode.printBytes(refOp, TestWriterCode.fileArr, pos, actualRead);
                TestWriterCode.printBytes(sipOp, readBuf, ofs, actualRead);
            }
            if(actualRead > 0)
            {
                if(actualRead < len)
                    prevShort = true;
                refOp.println("Id: " + i + ' ' + opMsg + pos + " - " + (pos+actualRead));
                sipOp.println("Id: " + i + ' ' + opMsg + pos + " - " + (pos+actualRead));
            }
            else
            {
                refOp.println("Id: " + i + " End Of Stream actualRead = " + actualRead);
                sipOp.println("Id: " + i + " End Of Stream actualRead = " + actualRead);
            }
            if(TestWriterCode.flush)
            {
                refOp.flush();
                sipOp.flush();
            }
            if(actualRead > 0)
                pos += actualRead;
            i++;
        } while (actualRead >= 0);
        refOp.close();
        sipOp.close();

        System.out.println("Max Buf Size = " + sip.getMaxBufSize());
        tc.closeFiles();
        tc = null;

        TestWriterCode.fileArr = null;
    }
}
