#include <string>
#include <iostream>
#include <fstream>
#include <vector>
#include <regex>
#include <unordered_map>

int main(int argc, char *argv[]) {

    if (argc != 2) {
        std::cout << "Usage: " << argv[0] << " path_to_dataset" << std::endl;
        return EXIT_FAILURE;
    }

    std::ifstream in(argv[1]);
    std::string data((std::istreambuf_iterator<char>(in)), std::istreambuf_iterator<char>());

    std::regex whitespace("\\s+");
    
    std::vector<std::string*> words;

    std::for_each(std::sregex_token_iterator(data.begin(), data.end(), whitespace, -1), std::sregex_token_iterator(), [&](auto s){
        words.push_back(new std::string(s));
    });

    std::cout << "num origin words: " << words.size() << std::endl;

    std::vector<std::string*> tmp;

    for (auto w: words) {
        std::regex reg("[^a-zA-Z0-9]+");
        auto w_ = std::regex_replace(*w, reg, "");
        if (w_ != "") {
            tmp.push_back(new std::string(w_));
        }
        delete w;
    }

    words.assign(tmp.begin(), tmp.end());
    tmp.clear();    

    for (auto w: words) {
        auto s = *w;
        std::transform(s.begin(), s.end(), s.begin(), [](auto c){ return std::tolower(c); });
        tmp.push_back(new std::string(s));
        delete w;
    }

    words.assign(tmp.begin(), tmp.end());
    tmp.clear();

    std::cout << "num filtered words: " << words.size() << std::endl;

    std::unordered_map<std::string, int> wordCount;

    for (auto w: words) {
        wordCount[*w]++;
        delete w;
    }

    std::cout << "num different words: " << wordCount.size() << std::endl;

    return 0;

}
