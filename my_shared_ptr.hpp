#ifndef __MY_SHARED_PTR_HPP__
#define __MY_SHARED_PTR_HPP__

#include <cassert>
#include <iostream>

template <typename T>
struct my_shared_ptr {
    
private:

    T *data;
    int *counter;

    static int increase_count, decrease_count;
    
    void decrease_ref() {
        (*counter)--;
#ifdef DEBUG
        std::cerr << data << " decrease to " << *counter << std::endl;
#endif
        if (*counter == 0) {
#ifdef DEBUG
            std::cerr << data << " destructs" << std::endl;
#endif
            delete data;
            delete counter;
        }
        ++decrease_count;
    }

    void increase_ref() {
        (*counter)++;
#ifdef DEBUG
        std::cerr << data << " increase to " << *counter << std::endl;
#endif
        ++increase_count;
    }

public:

    my_shared_ptr() = delete;

    my_shared_ptr(T *data): data(data), counter(new int(0)) {
        assert(data != nullptr);
#ifdef DEBUG
        std::cerr << data << " constructs" << std::endl;
#endif
        increase_ref();
    }

    my_shared_ptr(const my_shared_ptr<T> &rhs): counter(rhs.counter), data(rhs.data) {
        increase_ref();
    }

    my_shared_ptr<T>& operator=(const my_shared_ptr<T> &rhs) {
        if (&rhs == this) return *this;
        decrease_ref();
        this->data = rhs.data;
        this->counter = rhs.counter;
        increase_ref();
        return *this;
    }

    ~my_shared_ptr() {
        decrease_ref();
    }

    T* get() {
        return data;
    }

    T* operator->() {
        return data;
    }

    T& operator*() {
        return *data;
    }

    int get_decrease_count() { return decrease_count; }
    
    int get_increase_count() { return increase_count; }

};

template <typename T> int my_shared_ptr<T>::increase_count = 0;
template <typename T> int my_shared_ptr<T>::decrease_count = 0;

#endif
