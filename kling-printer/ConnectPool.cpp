#include "ConnectPool.h"

ConnectPool::ConnectPool(std::size_t max, std::string host, int port, long connectMS, long readMS) 
: max_(max) , host_(host), port_(port), cto_(connectMS), rto_(readMS) {}

    // 归还连接
void ConnectPool::release(std::shared_ptr<HttpsConn> conn) {
    if (!conn) return;
    std::lock_guard lg(m_);
    if (pool_.size() < max_) {
        pool_.push(std::move(conn));
    }
    // 超过 max_ 则直接销毁（shared_ptr 析构）
}

    // 借出连接
std::shared_ptr<HttpsConn> ConnectPool::acquire() {
    {
        std::lock_guard lg(m_);
        if (!pool_.empty()) {
            auto ptr = std::move(pool_.front());
            pool_.pop();
            return ptr;
        }
    }
    return create(host_, cto_, rto_);
}

std::shared_ptr<HttpsConn> ConnectPool::create(const std::string& host, long connectMS, long readMS) {
    auto c = std::make_shared<HttpsConn>();
    c->cli = std::make_unique<httplib::SSLClient>(host.c_str());
    c->cli->set_connection_timeout(connectMS / 1000, (connectMS % 1000) * 1000);
    c->cli->set_read_timeout(readMS / 1000, (readMS % 1000) * 1000);
    return c;
}