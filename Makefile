SOURCES := decode.c main.c wav.c
HEADERS := decode.h platform.h wav.h
GCCOPTS := -Wpedantic -Wall -Werror -Wextra

rdac2wav: $(SOURCES) $(HEADERS)
	gcc $(GCCOPTS) -o$@ $(SOURCES)

clean:
	rm -f rdac2wav
