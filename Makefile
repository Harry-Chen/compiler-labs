SOURCES=$(wildcard *.cpp)
HEADERS=$(wildcard *.hpp)
TARGETS=$(SOURCES:.cpp=)
RESULTS=$(SOURCES:.cpp=.txt)

CFLAGS := -O3 -g -Wall

.PHONY: all results clean resultsclean distclean

all: $(TARGETS)

results: $(RESULTS)

%: %.cpp $(HEADERS)
	$(CXX) $(CFLAGS) -o $@ $<

%.txt: %
	sudo perf stat -B -e cache-references,cache-misses,cycles,instructions,branches,faults,migrations /usr/bin/time -v ./$< datasets/wiki0.01.txt 2>&1 | tee $@

clean:
	rm -f $(TARGETS)

resultsclean:
	rm -f $(RESULTS)

distclean: clean resultsclean


