#include "HttpClient.h"

using namespace httplib;

#include "HttpClient.h"

// ---------- 连接包装 ----------
// struct ConnFactory {
//     static 
// };

// ---------- ApiClient 实现 ----------
HttpClient::HttpClient(const std::string& host, std::size_t pool_size)
    : host_(host) {
    pool_ = std::make_unique<ConnectPool>(pool_size, host_, connTimeout_, readTimeout_);
}

void HttpClient::setToken(const std::string& token) { token_ = token; }
void HttpClient::setTimeout(long c, long r) { connTimeout_ = c; readTimeout_ = r; }

template <typename Conn>
static json doRequest(Conn& conn,
                      const std::string& token,
                      const std::string& path,
                      const json& body,
                      const std::vector<std::pair<std::string, std::string>>& headers,
                      bool isPost) {

    httplib::Headers hdrs(headers.begin(), headers.end());
    if (!token.empty()) hdrs.emplace("Authorization", "Token " + token);
    hdrs.emplace("Content-Type", "application/json");

    httplib::Result res;
    if (isPost) {
        res = conn->Post(path.c_str(), hdrs, body.dump(), "application/json");
    } else {
        res = conn->Get(path.c_str(), hdrs);
    }

    if (!res) throw std::runtime_error("transport error");
    if (res->status != 200) throw std::runtime_error("HTTP " + std::to_string(res->status));
    return json::parse(res->body);
}

json HttpClient::get(const std::string& path,
                    const std::vector<std::pair<std::string, std::string>>& headers) {
    auto conn = pool_->acquire();
    auto ret = doRequest(conn->cli, token_, path, json(), headers, false);
    pool_->release(std::move(conn));
    return ret;
}

json HttpClient::post(const std::string& path,
                     const json& body,
                     const std::vector<std::pair<std::string, std::string>>& headers) {
    auto conn = pool_->acquire();
    auto ret = doRequest(conn->cli, token_, path, body, headers, true);
    pool_->release(std::move(conn));
    return ret;
}

// 更新图片的状态
// bool HttpClient::updateImageStatus(const std::string& status) {

// }

// bool HttpClient::DownloadImage(const std::string& imgUrl) {

// }
// bool HttpClient::fetchImageUrl() {

// }