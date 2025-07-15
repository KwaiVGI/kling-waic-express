#pragma once
#include <queue>
#include <mutex>
#include <memory>
#include <chrono>
#include "httplib.h"

struct HttpsConn {
        std::unique_ptr<httplib::SSLClient> cli;
};

class ConnectPool {
public:
    using Clock = std::chrono::steady_clock;

    explicit ConnectPool(std::size_t max = 8, std::string host_, long connectMS, long readMS);
    
    ~ConnectPool() = default;
    
    std::shared_ptr<HttpsConn> acquire();

    void release(std::shared_ptr<HttpsConn> conn);


private:
    std::shared_ptr<HttpsConn> create(const std::string& host, long connectMS, long readMS);
    std::queue<std::shared_ptr<HttpsConn>> pool_;
    std::mutex m_;
    std::size_t max_;
    std::string host_;
    long cto_;
    long rto_;
};