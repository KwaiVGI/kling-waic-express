#include "HttpClient.h"
#include <fstream>
#include <QFile>
#include <QBuffer>
#include <QImageReader>
#include <QString>
#include <QFileInfo>
#include <QDir>
using namespace httplib;

const std::string HttpClient::DOWNLOAD_PATH = "/cppcode/kling-waic-express/kling-printer/download/";

std::string stringPrefix(const std::string& s) {
    static const std::regex prefix(R"(^https://waic-api.klingai.com::6443)");
    return std::regex_replace(s, prefix, "");
}
HttpClient::HttpClient() {
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
    //     std::cout << "res error " << res.error() << std::endl;
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
    std::cout << "getJson:" << path << " result status:" << ret->status << std::endl;
    return json::parse(ret->body);
}

json HttpClient::postJson(const std::string& path,
                    const json& body,
                    const std::vector<std::pair<std::string, std::string>>& headers) {
    auto conn = pool_->acquire();
    httplib::Result ret = doRequest(conn->cli, token_, path, body, headers, true);
    pool_->release(std::move(conn));
    std::cout << "postJoson:" << path << " result status:" << ret->status << std::endl;
    if (!ret || ret->status != 200) {
        std::cout << "postJson return empty json" << std::endl;
        return json::object();
    }
    return json::parse(ret->body);
}

QImage HttpClient::getImage(const std::string& path,
                    const std::vector<std::pair<std::string, std::string>>& headers) {
    std::cout << "doGetImage" << std::endl;
    for (auto header : headers) {
        std::cout << header.first << " " << header.second << std::endl;
    }
    auto conn = pool_->acquire();
    httplib::Result ret = doRequest(conn->cli, "", path, json(), headers, false);
    std::cout << "endRequest" << std::endl;
    pool_->release(std::move(conn));
  
    if (!ret || ret->status != 200) {
        std::cout << "HTTP request failed" << std::endl;
    }
    if (auto ct = ret->get_header_value("Content-Type");
        ct.find("image/jpeg") == std::string::npos) {
        std::cout << "Content-Type not image/jpeg:" << ct.c_str() << std::endl;
        return {};
    }
    QByteArray raw(ret->body.data(), static_cast<int>(ret->body.size()));
    QImage img;
    QBuffer buf(&raw);
    buf.open(QIODevice::ReadOnly);
    QImageReader r(&buf, "JPEG");
    if (!r.canRead())
        std::cout << "ImageRader use failed" << std::endl;
        std::cout << r.errorString().toStdString() << std::endl;
    img = r.read();
    return img;
}

bool HttpClient::fetchImageQueue() {
    std::cout << "[INFO] begin fetch Image Queue" << std::endl;
    json ret = postJson("/api/printings/fetch", {});
    std::cout << "post success!" << std::endl;
    if (ret.empty()) {
        return false;
    }
    if (ret["status"] == "FAILED") {
        std::cout << "HTTP ERROR! CANNOT CONNECT" << std::endl;
        return false;
    }
    std::cout << ret["data"] << std::endl;

    if (!ret.contains("data") || ret["data"].is_null() || ret["status"] != "SUCCEED") {
        // 队列为空
        std::cout << "[INFO] No data." << std::endl;
        return false;
    }
    long long id = ret.at("data").at("id");
    std::cout << "id" << id << std::endl;
    std::string name = std::to_string(id);
    std::cout << "name:" << name << std::endl;
    std::string download_url = stringPrefix(ret.at("data").at("task").at("outputs").at("url"));
    std::cout << "[INFO] ready to download. name: " + name + " url:" + download_url << std::endl;
    if (!downloadImage(download_url, DOWNLOAD_PATH, name + ".jpg")) {
        std::cout << "[INFO] DownLoad image failed. url:" << download_url << std::endl; 
        return false;
    }
    std::cout << "[INFO] download Image Success. name:" << name << std::endl;
    return true;
}

bool HttpClient::downloadImage(const std::string& imgUrl, const std::string& dir, const std::string& name) {
    std::vector<std::pair<std::string, std::string>> hdrs;
    // hdrs.push_back(std::make_pair("nocache", "1"));
    QImage img = getImage(imgUrl, hdrs);
    if (img.isNull()) {
        return false;
    }
    std::cout << "[INFO] ready to save. imgUrl:" + imgUrl + "dir:" + dir + " fileName:" + name << std::endl;
    QFileInfo fi(QString::fromStdString(dir));
    std::cout << fi.absoluteFilePath().toStdString() << std::endl;
    QDir().mkpath(fi.absolutePath());
    return img.save(QString::fromStdString(dir + name), "JPG", 100);
}

bool HttpClient::updateImageStatus() {
    return true;
}