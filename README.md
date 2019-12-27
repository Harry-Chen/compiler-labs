# Experiment 3 for Compiler (II)

Testing and analysis of GC overhead in Go, comparing with reference counting and manual memory management in C++.

## Source Code

* `WordCound.go`: original version, in Go
* `wordcount_refcount.cpp`: C++ version, using a handcraft reference counting library `my_shared_ptr.hpp`
* `wordcount_sharedptr.cpp`: C++ version, using `std::shared_ptr`
* `wordcount_manual.cpp`: C++ version, using nothing but `std::string`
* `wordcount_silly.cpp`: C++ version, using manual `new/free` of `std::string*`

## Commands

* `make`: build all executables
* `make results`: run all tests
* `make clean`: remove all executables
* `make resultsclean`: remove all test results
* `make distclean`: remove everything generated
