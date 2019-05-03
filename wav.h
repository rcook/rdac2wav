#ifndef __WAV_H_INCLUDED
#define __WAV_H_INCLUDED

#include <stdio.h>

void writeWavHeader(FILE *fout, int numSamples, int sampleRate, int bitDepth, int numChannels);

void writeWavSamples16(FILE *fout, int samples[]);

void writeWavSamples16as24(FILE *fout, int samples[]);

void writeWavSamples16as24Stereo(FILE *fout, int samples[]);

void writeWavSamples16Stereo(FILE *fout, int samples[]);

void writeWavSamples24(FILE *fout, int samples[]);

void writeWavSamples24as16(FILE *fout, int samples[]);

#endif
