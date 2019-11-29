compile: clean
	mkdir build
	javac -Xlint:-options -source 1.6 -target 1.6 -Xlint:deprecation \
	  -cp lib/joeq.jar \
	  -sourcepath src -d build `find src -name "*.java"`

clean:
	rm -rf build/
