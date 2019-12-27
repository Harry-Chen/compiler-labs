SOURCES=$(wildcard *.cpp)
HEADERS=$(wildcard *.hpp)
TARGETS=$(SOURCES:.cpp=)
RESULTS=$(SOURCES:.cpp=.txt)

CFLAGS := -O3 -g -Wall

all: $(TARGETS)

result: $(RESULTS)

%: %.cpp $(HEADERS)
	$(CXX) $(CFLAGS) -o $@ $<

%.txt: %
	/usr/bin/time -v $< dataset/wiki0.001.txt | tee $@

clean:
	rm -f $(TARGETS)

