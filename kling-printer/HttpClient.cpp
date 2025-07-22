#include "HttpClient.h"
#include <glog/logging.h>
#include <fstream>
#include <QFile>
#include <QBuffer>
#include <QImageReader>
#include <QString>
#include <QFileInfo>
#include <QDir>
using namespace httplib;


// std::string stringPrefix(const std::string& s) {
//     static const std::regex prefix(R"(^https://waic-api.klingai.com::6443)");
//     return std::regex_replace(s, prefix, "");
// }

HttpClient::HttpClient(const std::string& host, int port, const std::string& token) 
: host_(host), port_(port), token_(token)  {
    pool_ = std::make_unique<ConnectPool>(5, host_, port_, connTimeout_, readTimeout_);
}

template <typename Conn>
static Result doRequest(Conn& conn,
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
    // if (!res) {
    //     LOG(INFO) << "res error " << res.error() << std::endl;
    //     return res;
    //     // throw std::runtime_error("transport error");
    // }
    // if (res->status != 200) {
    //     return res;
    //     // throw std::runtime_error("HTTP " + std::to_string(res->status));
    // }
    return res; 
}

json HttpClient::getJson(const std::string& path,
                    const std::vector<std::pair<std::string, std::string>>& headers) {
    auto conn = pool_->acquire();
    httplib::Result ret = doRequest(conn->cli, token_, path, json(), headers, false);
    pool_->release(std::move(conn));
    LOG(INFO) << "getJson:" << path << " result status:" << ret->status << std::endl;
    if (!ret || ret->status != 200) {
        return json::object();
    }
    return json::parse(ret->body);
}

json HttpClient::postJson(const std::string& path,
                    const json& body,
                    const std::vector<std::pair<std::string, std::string>>& headers) {
    auto conn = pool_->acquire();
    httplib::Result ret = doRequest(conn->cli, token_, path, body, headers, true);
    pool_->release(std::move(conn));
    LOG(INFO) << "postJoson:" << path << " result status:" << ret->status << std::endl;
    if (!ret || ret->status != 200) {
        LOG(INFO) << "postJson return empty json" << std::endl;
        return json::object();
    }
    return json::parse(ret->body);
}

QImage HttpClient::getImage(const std::string& path,
                    const std::vector<std::pair<std::string, std::string>>& headers) {
    LOG(INFO) << "Getting Image" << path << std::endl;
    auto conn = pool_->acquire();
    httplib::Result ret = doRequest(conn->cli, token_, path, json(), headers, false);
    LOG(INFO) << "endRequest" << std::endl;
    pool_->release(std::move(conn));
  
    if (!ret || ret->status != 200) {
        LOG(INFO) << "HTTP request failed" << std::endl;
    }
    if (auto ct = ret->get_header_value("Content-Type");
        ct.find("image/jpeg") == std::string::npos) {
        LOG(INFO) << "Content-Type not image/jpeg:" << ct.c_str() << std::endl;
        return {};
    }
    QByteArray raw(ret->body.data(), static_cast<int>(ret->body.size()));
    QImage img;
    QBuffer buf(&raw);
    buf.open(QIODevice::ReadOnly);
    QImageReader r(&buf, "JPEG");
    if (!r.canRead())
        LOG(INFO) << "ImageRader use failed" << std::endl;
        LOG(INFO) << r.errorString().toStdString() << std::endl;
    img = r.read();
    return img;
}

json HttpClient::fetchImageQueue() {
    LOG(INFO) << "[INFO] begin fetch Image Queue" << std::endl;
    json ret = postJson("/api/printings/fetch", {});
    LOG(INFO) << "post success! json:" << ret << std::endl;
    // if (ret.empty()) {
    //     return ret;
    // }
    // if (ret["status"] == "FAILED") {
    //     LOG(INFO) << "HTTP ERROR! CANNOT CONNECT" << std::endl;
    //     return ret;
    // }
    // LOG(INFO) << ret["data"] << std::endl;

    // if (!ret.contains("data") || ret["data"].is_null() || ret["status"] != "SUCCEED") {
    //     // 队列为空
    //     LOG(INFO) << "[INFO] No data." << std::endl;
    //     return ret;
    // }
    // long long id = ret.at("data").at("id");
    // LOG(INFO) << "id" << id << std::endl;
    // std::string name = std::to_string(id);
    // LOG(INFO) << "name:" << name << std::endl;
    // std::string download_url = ret.at("data").at("task").at("outputs").at("url");
    return ret;
}

bool HttpClient::downloadImage(const std::string& imgUrl, const std::string& dir, const std::string& name) {
    std::vector<std::pair<std::string, std::string>> hdrs;
    // hdrs.push_back(std::make_pair("nocache", "1"));
    QImage img = getImage(imgUrl, hdrs);
    if (img.isNull()) {
        return false;
    }
    LOG(INFO) << "[INFO] ready to save. imgUrl:" + imgUrl + "dir:" + dir + " fileName:" + name << std::endl;
    QFileInfo fi(QString::fromStdString(dir));
    LOG(INFO) << fi.absoluteFilePath().toStdString() << std::endl;
    QDir().mkpath(fi.absolutePath());
    return img.save(QString::fromStdString(dir + name), "JPG", 100);
}

bool HttpClient::updateImageStatus() {
    return true;
}