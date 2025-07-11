#include "include/httplib.h"

using namespace httplib;

enum class ImageStatus {
    QUEUING,
    PRINTING,
    COMPLETED
};

class HttpClient {
private:
    HttpClient();
    ~HttpClient();
public:
    bool updateImageStatus();

    bool fetchImage();
};