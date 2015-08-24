/* MediaUtil LLJTran - $RCSfile: SplitInputStreamTester.java,v $
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
 *  $Id: SplitInputStreamTester.java,v 1.4 2005/08/18 04:35:34 drogatkin Exp $
 *
 */
package mediautil.test;

import java.io.*;
import java.util.Random;

import mediautil.gen.directio.IterativeReader;
import mediautil.gen.directio.SplitInputStream;

class TestCode implements IterativeReader
{
    public static byte fileArr[];
    public static Random r;
    public static int id;
    private InputStream ip;
    private int pos, n, skipProb, maxBuf, maxReads;
    private PrintStream refOp, sipOp;
    private byte readBuf[];
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

    public TestCode(InputStream ip, int n, int skipProb, int maxBuf,
                    int maxReads) throws FileNotFoundException
    {
        this.ip = ip;
        this.n = n;
        this.skipProb = skipProb;
        this.maxBuf = maxBuf;
        this.maxReads = maxReads;
        pos = 0;
        readBuf = new byte[maxBuf];
        refOp = new PrintStream(new BufferedOutputStream(new FileOutputStream("sub" + n + "_ref.txt"), 100000));
        sipOp = new PrintStream(new BufferedOutputStream(new FileOutputStream("sub" + n + "_sip.txt"), 100000));
    }

public static boolean debug = false;

    private boolean prevShort = false;
    public int nextRead(int numBytes)
    {
        int readCount = r.nextInt(maxReads) + 1;
        int len = 0, ofs, actualRead;
        boolean isSkip = false;
        int retVal = IterativeReader.CONTINUE;
      try {
        do
        {
            len = r.nextInt(maxBuf) + 1;
            isSkip = r.nextInt(16) < skipProb;
            String opMsg = isSkip?"Skipped ":"Read ";
            refOp.println("Trying Id: " + id + ' ' + opMsg + pos + " - " + (pos+len));
            sipOp.println("Trying Id: " + id + ' ' + opMsg + pos + " - " + (pos+len));
            if(flush)
            {
                refOp.flush();
                sipOp.flush();
            }

            if(isSkip)
                actualRead = (int)ip.skip(len);
            else {
                ofs = TestCode.r.nextInt(maxBuf-len+1);
                actualRead = ip.read(readBuf, ofs, len);
                printBytes(refOp, TestCode.fileArr, pos, actualRead);
                printBytes(sipOp, readBuf, ofs, actualRead);
            }
            if(actualRead > 0)
            {
                if(actualRead < len)
                    prevShort = true;
                refOp.println("Id: " + id + " readCount: " + readCount + ' ' + opMsg + pos + " - " + (pos+actualRead));
                sipOp.println("Id: " + id + " readCount: " + readCount + ' ' + opMsg + pos + " - " + (pos+actualRead));
            }
            else
            {
                refOp.println("Id: " + id + " readCount: " + readCount + ' ' + " End Of Stream actualRead = " + actualRead);
                sipOp.println("Id: " + id + " readCount: " + readCount + ' ' + " End Of Stream actualRead = " + actualRead);
            }
            if(flush)
            {
                refOp.flush();
                sipOp.flush();
            }
            if(actualRead > 0)
                pos += actualRead;
            --readCount;
        } while(actualRead >= 0 && readCount > 0);
        if(actualRead < 0)
        {
            retVal = IterativeReader.STOP;
            refOp.close();
            sipOp.close();
        }
      } catch(Exception e)
      {
        e.printStackTrace(System.err);
        sipOp.println("Exception for Id: " + id + " isSkip = " + isSkip + " readCount: " + readCount + ' ' + " Len = " + len);
        e.printStackTrace(sipOp);
        sipOp.flush();
        throw new RuntimeException("What the Heaven");
      }
        return retVal;
    }

    public void closeFiles()
    {
        refOp.close();
        sipOp.close();
        refOp = null;
        sipOp = null;
    }
}

// Like ByteArrayInputStream, but actaully read/skipped  value is a random
// number upto  value supplied which is allowed by the InputStream spec but
// ByteArrayInputStream does not do.
class CrankyStream extends InputStream
{
    byte readBuf[];
    int off, len, pos, avail;
    public CrankyStream(byte readBuf[], int off, int len)
    {
        this.readBuf = readBuf;
        this.off = off;
        this.len = len;
        pos = off;
        avail = len;
    }

    public int read(byte b[], int off, int len) throws IOException
    {
        if(len < 0)
            throw new IndexOutOfBoundsException("Negative Length Read attempted, len = " + len);
        byte b1 = b[off], b2 = b[off + len -1];
        if(len == 0)
            return 0;

        int retVal = len;

        if(len > avail)
            retVal = avail;

        if(retVal <= 0)
            retVal = -1;
        else
        {
            retVal = TestCode.r.nextInt(retVal) + 1;
            System.arraycopy(readBuf, pos, b, off, retVal);
            pos += retVal;
            avail -= retVal;
        }

        return retVal;
    }

    private byte oneByteArr[] = new byte[1];
    public int read() throws IOException
    {
        int retVal = -1;
        if(read(oneByteArr) == 1)
            retVal = oneByteArr[0] & 255;
        return retVal;
    }

    public long skip(long n) throws IOException
    {
        long retVal = n;

        if(n > avail)
            retVal = avail;

        if(retVal > 0)
        {
            retVal = TestCode.r.nextInt((int)retVal + 1);
            pos += (int)retVal;
            avail -= (int)retVal;
        }
        else
            retVal = 0;

        return retVal;
    }

    /**
     * Does not strictly conform to InputStream spec since it always returns
     * atleast 1 unless the end of file is reached.
     **/
    public int available() throws IOException
    {
        return avail>0?1:0;
    }
}

public class SplitInputStreamTester extends SplitInputStream {

    public SplitInputStreamTester()
    {
        super(null);
    }

    public static void main(String args[]) throws Exception
    {
        File finfo = new File(args[0]);
        int size = (int)finfo.length();
        FileInputStream fip = new FileInputStream(finfo);
        TestCode.fileArr = new byte[size];
        int readLen = fip.read(TestCode.fileArr);
        int i;
        fip.close();
        // ByteArrayInputStream ip = new ByteArrayInputStream(TestCode.fileArr, 0, readLen);
        CrankyStream ip = new CrankyStream(TestCode.fileArr, 0, readLen);
        int numSubReaders = 5, skipProb = 10, maxBuf = 20;
        SplitInputStream sip = new SplitInputStream(ip, 20, 7);
        TestCode.r = new Random(555);
        i = 0;
        TestCode tc[] = new TestCode[numSubReaders];
        do
        {
            InputStream subIp = sip.createSubStream(10, 10);
            tc[i] = new TestCode(subIp, i, skipProb, maxBuf, 4);
            sip.attachSubReader(tc[i], subIp);
            i++;
        }while(i < numSubReaders);
        PrintStream refOp = new PrintStream(new BufferedOutputStream(new FileOutputStream("main_ref.txt"), 100000));
        PrintStream sipOp = new PrintStream(new BufferedOutputStream(new FileOutputStream("main_sip.txt"), 100000));
        boolean prevShort = false;
        int pos, len, ofs, actualRead;
        byte readBuf[] = new byte[1001];
        pos = 0;
        i = 0;
        do
        {
            len = TestCode.r.nextInt(1001) + 1;
            boolean isSkip = TestCode.r.nextInt(16) < skipProb;
            String opMsg = isSkip?"Skipped ":"Read ";
            TestCode.id = i;
            refOp.println("Trying Id: " + i + ' ' + opMsg + pos + " - " + (pos+len));
            sipOp.println("Trying Id: " + i + ' ' + opMsg + pos + " - " + (pos+len));
            if(TestCode.flush)
            {
                refOp.flush();
                sipOp.flush();
            }
            if(isSkip)
                actualRead = (int)sip.skip(len);
            else {
                ofs = TestCode.r.nextInt(1001-len+1);
                actualRead = sip.read(readBuf, ofs, len);
                TestCode.printBytes(refOp, TestCode.fileArr, pos, actualRead);
                TestCode.printBytes(sipOp, readBuf, ofs, actualRead);
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
            if(TestCode.flush)
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

        System.out.println("Max Blocks = " + sip.getMaxBufSize());
        i = 0;
        do
        {
            tc[i].closeFiles();
            tc[i] = null;
            i++;
        }while(i < numSubReaders);

        ip = null;
        TestCode.fileArr = null;
    }
}
