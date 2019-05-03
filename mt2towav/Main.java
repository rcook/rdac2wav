package org.rcook;

import java.io.*;
import java.util.*;

//
// MT2 to WAV File Converter Version 0.9
//
// Copyright (c) 2006, Randy Gordon (randy@integrand.com)
// Licensed as LGPL
//

public class MT2Wav {

    public static int BITMASK[] = {0x80,0x40,0x20,0x10,0x08,0x04,0x02,0x01};

    //**************************************** PATTERNS ***********************
    private static String[] patterns = {
            // Indicator       Exponent    Layout       Linears   Mantissa Change
            "00..00..",        "0",        "A",         "2",      "0",
            "00..01..",        "1",        "A",         "2",      "0",
            "00..10..",        "2",        "A",         "2",      "0",
            "00..11..",        "3",        "A",         "2",      "0",
            "01..00..",        "4",        "A",         "2",      "0",
            "01..01..",        "5",        "A",         "2",      "0",

            "01..10..",        "4",        "B",         "4",      "0",
            "01..11..",        "5",        "B",         "4",      "0",
            "10..00..",        "6",        "B",         "4",      "0",
            "10..01..",        "7",        "B",         "4",      "0",
            "10..10..",        "8",        "B",         "4",      "0",
            "10..11..",        "9",        "B",         "4",      "0",

//  "110.000.",        "?",        "?",         "?",      "?", // unknown

            "110.001.",        "0",        "F",         "2",      "0",
            "110.010.",        "1",        "F",         "2",      "0",
            "110.011.",        "2",        "F",         "2",      "0",
            "110.100.",        "3",        "F",         "2",      "0",
            "110.101.",        "4",        "F",         "2",      "0",

            "110.110.",        "6",        "G",         "2",      "0",

            "110.111.",        "2",        "E",         "2",      "0",
            "111.000.",        "3",        "E",         "2",      "0",
            "111.001.",        "4",        "E",         "2",      "0",
            "111.010.",        "5",        "E",         "2",      "0",
            "111.011.",        "6",        "E",         "2",      "0",
            "111.100.",        "7",        "E",         "2",      "0",

            "11101010",        "6",        "C",         "8",      "0",
            "11101011",        "7",        "C",         "8",      "0",
            "11101100",        "8",        "C",         "8",      "0",
            "11101101",        "9",        "C",         "8",      "0",
            "11101110",        "10",       "C",         "8",      "0",

            "11101111",        "10",       "C",         "16",     "1",

            "11111010",        "8",        "D",         "4",      "0",

//  "11111011",        "?",        "?",         "?",      "?",  // unknown
//  "11111100",        "?",        "?",         "?",      "?",  // unknown
//  "11111101",        "?",        "?",         "?",      "?",  // unknown
//  "11111110",        "?",        "?",         "?",      "?",  // unknown
//  "11111111",        "?",        "?",         "?",      "?",  // unknown

    };

    private static HashMap patternExponentMap       = new HashMap();
    private static HashMap patternLayoutMap         = new HashMap();
    private static HashMap patternLinearMap         = new HashMap();
    private static HashMap patternMantissaChangeMap = new HashMap();

//**************************************** BITS ***************************

    public static String PATTERN_A =
            "pp88888888888777ppgggggggggggfff" +
                    "7666666555544444feeeeeeddddccccc" +
                    "4433332222221111ccbbbbaaaaaa9999";

    public static String PATTERN_B =
            "pp88888887777766ppgggggggfffffee" +
                    "6666555554444444eeeedddddccccccc" +
                    "3333322222211111bbbbbaaaaaa99999";


    public static String PATTERN_C =
            "pppp888888777776ppppggggggfffffe" +
                    "6666655555444444eeeeedddddcccccc" +
                    "3333322222211111bbbbbaaaaaa99999";

    public static String PATTERN_D =
            "pppp888888887777ppppggggggggffff" +
                    "6666665555444444eeeeeeddddcccccc" +
                    "4433332222221111ccbbbbaaaaaa9999";

    public static String PATTERN_E =
            "ppp8888888887777pppgggggggggffff" +
                    "6666665555444444eeeeeeddddcccccc" +
                    "4433332222221111ccbbbbaaaaaa9999";

    public static String PATTERN_F =
            "ppp8888888888887pppggggggggggggf" +
                    "7776666655554444fffeeeeeddddcccc" +
                    "4443333222221111cccbbbbaaaaa9999";

    public static String PATTERN_G =
            "ppp8888888888777pppggggggggggfff" +
                    "7666666555544444feeeeeeddddccccc" +
                    "4433332222221111ccbbbbaaaaaa9999";

    public static boolean isVS880 = false;

    public static boolean isDebug = false;

    //*****************************************************************************
    public static void main(String args[])
    {
        if (args.length < 2) {
            System.out.println("Usage:   java MT2Wav [mt2 file] [wav file]");
            System.out.println("Example: java MT2Wav take1.vs8 take1.wav");
            System.exit(0);
        }

        try {
            FileInputStream  in  = new FileInputStream(args[0]);
            FileOutputStream out = new FileOutputStream(args[1]);

            initializePatterns();

            // Get length of VS file
            File vsFile     = new File(args[0]);
            long fileSize   = vsFile.length();

            // Page size (for alignment)
            int pageSize      = isVS880 ? 32768 : 65536;
            int blocksPerPage = isVS880 ?  2730 :  5460;
            int blockSize     = 12; // MT2
            int pagePadBytes  = pageSize - blocksPerPage*blockSize;

            int numPages      = ((int)fileSize)/pageSize;
            int numBlocks     = numPages*blocksPerPage;

            // Write the WAV header
            int  numSamples = numBlocks*16;
            int  numBytes   = numSamples*2;
            writeWavHeader(out, numBytes);

            int  samples[]   = new int[17];
            byte block[]     = new byte[blockSize];
            byte dummy[]     = new byte[128];

            samples[0] = 0x00; // Initial d0 value for algorithm

            for (int i=0; i<numBlocks; i++) {
                in.read(block, 0, blockSize);

                String patternCode = getPatternCode(block);

                String layout           = getLayout(patternCode);
                int    exponent         = getExponent(patternCode);
                String linear           = getLinear(patternCode);
                int    mantissaChange   = getMantissaChange(patternCode);

                executeLayout(layout, exponent, mantissaChange, block, samples);

                sum(patternCode, samples);

                samples[0] = samples[16]; // d0 value for next iteration

                invert(samples);

                writeWavSamples(out, samples);

                // Check if at end of page boundary - if so, eat pad bytes.
                if ((i+1)%blocksPerPage == 0) in.read(dummy, 0, pagePadBytes);
            }

            in.close();
            out.close();
        }
        catch (Exception ex) {
            System.out.println("Conversion failed:" + ex);
        }
    }
    //*****************************************************************************
    public static void initializePatterns()
    {
        int numColumns = 5;
        for (int i=0; i<patterns.length/numColumns; i++) {
            String pattern        = patterns[i*numColumns];
            String exponent       = patterns[i*numColumns+1];
            String layout         = patterns[i*numColumns+2];
            String linear         = patterns[i*numColumns+3];
            String mantissaChange = patterns[i*numColumns+4];
            patternExponentMap.put(      pattern, exponent);
            patternLayoutMap.put(        pattern, layout);
            patternLinearMap.put(        pattern, linear);
            patternMantissaChangeMap.put(pattern, mantissaChange);
        }
    }
    //*****************************************************************************
    public static String getPatternCode(byte block[])
    {
        byte bx = (byte)((block[0]&0xf0) | (block[2]&0xf0)>>4);
        StringBuffer sb = new StringBuffer(toBinaryString(bx));
        if ("110".equals(sb.substring(0,3))) {
            sb.setCharAt(3,'.');
            sb.setCharAt(7,'.');
        }
        else if ("111".equals(sb.substring(0,3))) {
            if ("000".equals(sb.substring(4,7)) &#0124;&#0124;
            "001".equals(sb.substring(4,7)) &#0124;&#0124;
            "010".equals(sb.substring(4,7)) &#0124;&#0124;
            "011".equals(sb.substring(4,7)) &#0124;&#0124;
            "100".equals(sb.substring(4,7))) {
                sb.setCharAt(3,'.');
                sb.setCharAt(7,'.');
            }
        else {
                // Pattern code is 8 bits.
            }
        }
        else {
            sb.setCharAt(2,'.');
            sb.setCharAt(3,'.');
            sb.setCharAt(6,'.');
            sb.setCharAt(7,'.');
        }
        return sb.toString();
    }
    //*****************************************************************************
    public static int getExponent(String patternCode)
    {
        String exp = (String)patternExponentMap.get(patternCode);
        try { return Integer.parseInt(exp); } catch (Exception ex) { return 0; }
    }
    //*****************************************************************************
    public static String getLayout(String patternCode)
    {
        String layoutCode = (String)patternLayoutMap.get(patternCode);
        if ("A".equals(layoutCode)) return PATTERN_A;
        if ("B".equals(layoutCode)) return PATTERN_B;
        if ("C".equals(layoutCode)) return PATTERN_C;
        if ("D".equals(layoutCode)) return PATTERN_D;
        if ("E".equals(layoutCode)) return PATTERN_E;
        if ("F".equals(layoutCode)) return PATTERN_F;
        if ("G".equals(layoutCode)) return PATTERN_G;
        else                        return PATTERN_A;     //TBD
    }
    //*****************************************************************************
    public static String getLinear(String patternCode)
    {
        return (String)patternLinearMap.get(patternCode);
    }
    //*****************************************************************************
    public static int getMantissaChange(String patternCode)
    {
        String mc = (String)patternMantissaChangeMap.get(patternCode);
        try { return Integer.parseInt(mc); } catch (Exception ex) { return 0; }
    }
    //*****************************************************************************
    public static void executeLayout(String pattern, int exponent, int mantissaChange, byte in[], int out[])
    {
        int wordSize[] = new int[17];

        // Not a very efficient algorithm, but flexible.

        // Fill all bits from block into the sixteen samples.
        for (int i=1; i<=16; i++) out[i] = 0; // clear all new samples
        for (int i=0; i<pattern.length(); i++) {
            char ch = pattern.charAt(i);
            int outIndex = -1;
            switch (ch) {
                case '1': outIndex = 1; break;
                case '2': outIndex = 2; break;
                case '3': outIndex = 3; break;
                case '4': outIndex = 4; break;
                case '5': outIndex = 5; break;
                case '6': outIndex = 6; break;
                case '7': outIndex = 7; break;
                case '8': outIndex = 8; break;
                case '9': outIndex = 9; break;
                case 'a': outIndex = 10; break;
                case 'b': outIndex = 11; break;
                case 'c': outIndex = 12; break;
                case 'd': outIndex = 13; break;
                case 'e': outIndex = 14; break;
                case 'f': outIndex = 15; break;
                case 'g': outIndex = 16; break;
                default: break;
            }
            if (outIndex < 1 &#0124;&#0124; outIndex > 16) continue;
            int byteIndex = i/8;
            int bitIndex  = i%8;
            out[outIndex] <<= 1;
            out[outIndex] |= ((BITMASK[bitIndex]&in[byteIndex]) == 0 ? 0x00 : 0x01);
            wordSize[outIndex]++;
        }
        // Sign extend, exponentiate, and round all samples based on wordsize.
        for (int i=1; i<=16; i++) {
            out[i] = signExtend(out[i], wordSize[i]);
            out[i] = exponentiateAndRound(out[i], exponent);
            if (i%2 == 1) out[i] = changeMantissa(out[i], mantissaChange);
        }
    }
    //*****************************************************************************
    public static void sum(String patternCode, int out[])
    {
        String linear = (String)patternLinearMap.get(patternCode);
        if ("2".equals(linear))  sumLinear2(out);
        if ("4".equals(linear))  sumLinear4(out);
        if ("8".equals(linear))  sumLinear8(out);
        if ("16".equals(linear)) sumLinear16(out);

        limitForOverflow(out);
    }
    //*****************************************************************************
    public static void sumLinear2(int out[])
    {
        // Apply summation with linear values at position 8 and 16.
        // Note: previous d0 value is at position 0.
        out[4]  = out[4]  + (out[0]  + out[8])/2;
        out[2]  = out[2]  + (out[0]  + out[4])/2;
        out[6]  = out[6]  + (out[4]  + out[8])/2;
        out[12] = out[12] + (out[8]  + out[16])/2;
        out[10] = out[10] + (out[8]  + out[12])/2;
        out[14] = out[14] + (out[12] + out[16])/2;
        out[1]  = out[1]  + (out[0]  + out[2])/2;
        out[3]  = out[3]  + (out[2]  + out[4])/2;
        out[5]  = out[5]  + (out[4]  + out[6])/2;
        out[7]  = out[7]  + (out[6]  + out[8])/2;
        out[9]  = out[9]  + (out[8]  + out[10])/2;
        out[11] = out[11] + (out[10] + out[12])/2;
        out[13] = out[13] + (out[12] + out[14])/2;
        out[15] = out[15] + (out[14] + out[16])/2;
    }
    //*****************************************************************************
    public static void sumLinear4(int out[])
    {
        // Apply summation with linear values at positions 4, 8, 12, 16,
        // Note: previous d0 value is at position 0.
        out[2]  = out[2]  + (out[0]  + out[4])/2;
        out[6]  = out[6]  + (out[4]  + out[8])/2;
        out[10] = out[10] + (out[8]  + out[12])/2;
        out[14] = out[14] + (out[12] + out[16])/2;
        out[1]  = out[1]  + (out[0]  + out[2])/2;
        out[3]  = out[3]  + (out[2]  + out[4])/2;
        out[5]  = out[5]  + (out[4]  + out[6])/2;
        out[7]  = out[7]  + (out[6]  + out[8])/2;
        out[9]  = out[9]  + (out[8]  + out[10])/2;
        out[11] = out[11] + (out[10] + out[12])/2;
        out[13] = out[13] + (out[12] + out[14])/2;
        out[15] = out[15] + (out[14] + out[16])/2;
    }
    //*****************************************************************************
    public static void sumLinear8(int out[])
    {
        // Apply summation with linear values at positions 2,4,6,8,10,12,14,16.
        // Note: previous d0 value is  at position 0.
        out[1]  = out[1]  + (out[0]  + out[2])/2;
        out[3]  = out[3]  + (out[2]  + out[4])/2;
        out[5]  = out[5]  + (out[4]  + out[6])/2;
        out[7]  = out[7]  + (out[6]  + out[8])/2;
        out[9]  = out[9]  + (out[8]  + out[10])/2;
        out[11] = out[11] + (out[10] + out[12])/2;
        out[13] = out[13] + (out[12] + out[14])/2;
        out[15] = out[15] + (out[14] + out[16])/2;
    }
    //*****************************************************************************
    public static void sumLinear16(int out[])
    {
        // Apply summation with linear values at all positions.
        // Note: previous d0 value is  at position 0.
        // Essentially a NOOP.
    }
    //*****************************************************************************
    public static void limitForOverflow(int out[])
    {
        // Ensure no overflow conditions.
        for (int i=1; i<=16; i++) {
            if      (out[i] >=  32768) out[i] =  32767;
            else if (out[i] <= -32768) out[i] = -32767;
        }
    }
    //*****************************************************************************
    public static void invert(int out[])
    {
        for (int i=1; i<=16; i++) out[i] = -out[i];
    }
    //*****************************************************************************
    public static int signExtend(int x, int wordsize)
    {
        return ((x&(0x01<<(wordsize-1))) == 0) ? x : x|(0xffffffff<<wordsize);
    }
    //*****************************************************************************
    public static int exponentiateAndRound(int x, int exponent)
    {
        if (exponent >= 0) x <<= exponent;
        else               x >>= -exponent;
        if (exponent > 0)  x |= 0x01<<(exponent-1);
        return x;
    }
    //*****************************************************************************
    public static int changeMantissa(int x, int mantissaChange)
    {
        if (mantissaChange >= 0) x <<= mantissaChange;
        else                     x >>= -mantissaChange;
        return x;
    }
    //*****************************************************************************
    public static String toBinaryString(byte b)
    {
        StringBuffer sb = new StringBuffer();

        for (int i=0; i<8; i++) {
            sb.append((BITMASK[i]&b) == 0 ? "0" : "1");
        }
        return sb.toString();
    }
    //*****************************************************************************
    public static void writeWavHeader(FileOutputStream out, int numBytes)
            throws Exception
    {
        // Write the WAV header (mono, 16-bit, 44100Hz)
        int  numChunkBytes = numBytes + 38;

        byte chunkid[] = {0x52,0x49,0x46,0x46};
        out.write(chunkid, 0, 4);
        byte chunksize[] = {(byte)(numChunkBytes&0xff),
                (byte)(numChunkBytes>>8&0xff),
                (byte)(numChunkBytes>>16&0xff),
                (byte)(numChunkBytes>>24&0xff)};
        out.write(chunksize, 0, 4);
        byte format[] = {0x57,0x41,0x56,0x45};
        out.write(format, 0, 4);
        byte subchunk1id[] = {0x66,0x6d,0x74,0x20};
        out.write(subchunk1id, 0, 4);
        byte subchunk1size[] = {0x12,0x00,0x00,0x00};
        out.write(subchunk1size, 0, 4);
        byte audioformat[] = {0x01,0x00};
        out.write(audioformat, 0, 2);
        byte numchannels[] = {0x01,0x00};
        out.write(numchannels, 0, 2);
        byte samplerate[] = {0x44,(byte)0xac,0x00,0x00};
        out.write(samplerate, 0, 4);
        byte byterate[] = {0x24,(byte)0xf4,0x00,0x00};
        out.write(byterate, 0, 4);
        byte blockalign[] = {0x02,0x00};
        out.write(blockalign, 0, 2);
        byte bitspersample[] = {0x10,0x00};
        out.write(bitspersample, 0, 2);
        byte extraparamsize[] = {0x00,0x00};
        out.write(extraparamsize, 0, 2);
        byte subchunk2id[] = {0x64,0x61,0x74,0x61};
        out.write(subchunk2id, 0, 4);
        byte subchunk2size[] = {(byte)(numBytes&0xff),
                (byte)(numBytes>>8&0xff),
                (byte)(numBytes>>16&0xff),
                (byte)(numBytes>>24&0xff)};
        out.write(subchunk2size, 0, 4);
    }
    //*****************************************************************************
    public static void writeWavSamples(FileOutputStream out, int samples[])
            throws Exception
    {
        // Write it to the WAV (little-endian)
        for (int i=1; i<=16; i++) {
            out.write(samples[i]&0xff);
            out.write((samples[i]>>8)&0xff);
        }
    }

}

public class Main {

    public static void main(String[] args) {
	// write your code here
    }
}
