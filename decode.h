#ifndef __DECODE_H_INCLUDED
#define __DECODE_H_INCLUDED

void decodeCDR(unsigned char *in, int *out);

void decodeMTP(int d0, unsigned char *in, int *out);

void decodeMT1(int d0, unsigned char *in, int *out);

void decodeMT2(int d0, unsigned char *in, int *out);

void decodeM16(unsigned char *in, int *out);

void decodeM24(unsigned char *in, int *out);

#endif
