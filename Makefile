SOURCES := decode.c main.c wav.c
HEADERS := platform.h

rdac2wav: $(SOURCES) $(HEADERS)
	gcc -o$@ $(SOURCES)

clean:
	rm -f rdac2wav
