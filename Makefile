SOURCES := decode.c main.c wav.c
HEADERS := decode.h platform.h wav.h

rdac2wav: $(SOURCES) $(HEADERS)
	gcc -o$@ $(SOURCES)

clean:
	rm -f rdac2wav
