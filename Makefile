SOURCES=$(wildcard *.cpp)
HEADERS=$(wildcard *.hpp)
TARGETS=$(SOURCES:.cpp=)
RESULTS=$(SOURCES:.cpp=.txt)

CFLAGS := -O3 -g -Wall
PERF := sudo perf stat -B -e cache-references,cache-misses,cycles,instructions,branches,faults,migrations
TIME := /usr/bin/time -v
INPUT := datasets/wiki0.01.txt

.PHONY: all results clean resultsclean distclean

all: $(TARGETS) wordcount

wordcount: wordcount.go
	go build $<

results: wordcount $(RESULTS)
	$(PERF) $(TIME) env GOMAXPROCS=1 GOGC=off ./$< $(INPUT) 2>&1 | tee go_gc_off.txt
	$(PERF) $(TIME) env GOMAXPROCS=1 GOGC=on ./$< $(INPUT) 2>&1 | tee go_gc_on.txt
	$(PERF) $(TIME) env GOMAXPROCS=1 GOGC=on GODEBUG=gctrace=1 ./$< $(INPUT) 2>&1 | tee go_gc_on_debug.txt
	$(PERF) $(TIME) env GOMAXPROCS=88 GOGC=off ./$< $(INPUT) 2>&1 | tee go_gc_off_no_proc.txt
	$(PERF) $(TIME) env GOMAXPROCS=88 GOGC=on ./$< $(INPUT) 2>&1 | tee go_gc_on_no_proc.txt
	$(PERF) $(TIME) env GOMAXPROCS=88 GOGC=on GODEBUG=gctrace=1 ./$< $(INPUT) 2>&1 | tee go_gc_on_no_proc_debug.txt

%: %.cpp $(HEADERS)
	$(CXX) $(CFLAGS) -o $@ $<

%.txt: %
	$(PERF) $(TIME) ./$< $(INPUT) 2>&1 | tee $@

clean:
	rm -f $(TARGETS)

resultsclean:
	rm -f $(RESULTS)

distclean: clean resultsclean


