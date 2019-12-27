#include <string>
#include <iostream>
#include <fstream>
#include <vector>
#include <regex>
#include <unordered_map>
#include <memory>


using shared_string = std::shared_ptr<std::string>;

int main(int argc, char *argv[]) {

    if (argc != 2) {
        std::cout << "Usage: " << argv[0] << " path_to_dataset" << std::endl;
        return EXIT_FAILURE;
    }

    std::ifstream in(argv[1]);
    std::string data((std::istreambuf_iterator<char>(in)), std::istreambuf_iterator<char>());

    std::regex whitespace("\\s+");

    std::vector<shared_string> words;

    std::for_each(std::sregex_token_iterator(data.begin(), data.end(), whitespace, -1), std::sregex_token_iterator(), [&](auto s){
        words.push_back(std::make_shared<std::string>(s));
    });

    std::cout << "num origin words: " << words.size() << std::endl;

    std::vector<shared_string> tmp;

    std::regex reg("[^a-zA-Z0-9]+");

    for (auto w: words) {
        auto w_ = std::regex_replace(*w, reg, "");
        if (w_ != "") {
            tmp.push_back(std::make_shared<std::string>(w_));
        }
    }

    words.assign(tmp.begin(), tmp.end());
    tmp.clear();

    for (auto w: words) {
        std::string s = *w;
        std::transform(s.begin(), s.end(), s.begin(), [](auto c){ return std::tolower(c); });
        tmp.push_back(std::make_shared<std::string>(s));
    }
    
    words.assign(tmp.begin(), tmp.end());
    tmp.clear();

    std::cout << "num filtered words: " << words.size() << std::endl;

    std::unordered_map<shared_string, int> wordCount;

    for (auto w: words) {
        wordCount[w]++;
    }

    std::cout << "num different words: " << wordCount.size() << std::endl;


    return 0;
}
