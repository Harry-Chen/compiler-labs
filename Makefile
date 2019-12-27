SOURCES=$(wildcard *.cpp)
HEADERS=$(wildcard *.hpp)
TARGETS=$(SOURCES:.cpp=)
RESULTS=$(SOURCES:.cpp=.txt)

CFLAGS := -O3 -g -Wall

.PHONY: all result clean distclean

all: $(TARGETS)

result: $(RESULTS)

%: %.cpp $(HEADERS)
	$(CXX) $(CFLAGS) -o $@ $<

%.txt: %
	/usr/bin/time -v ./$< datasets/wiki0.001.txt | tee $@

clean:
	rm -f $(TARGETS)

distclean: clean
	rm -f $(RESULTS)

