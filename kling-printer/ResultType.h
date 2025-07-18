#ifndef RESULT_TYPE_H
#define RESULT_YTPE_H
#include <string>
#include <vector>
#include <chrono>

enum class PrintingStatus {
    SUBMITTED,
    QUEUING,
    PRINTING,
    COMPLETED
};

struct PrintingUpdateInput {
    PrintingStatus status;
};

// struct Printing {
//     long id;
//     std::string name;
//     Task task;
//     PrintingStatus status;
// };

struct PrintingUpdateInput {
    PrintingStatus status;
};

struct ImgInfo {
    std::string name;
    std::string path;
    PrintingStatus status;
};

// struct Task {
//     long long id;
//     std::string name;
//     std::vector<std::string> taskIds;
//     TaskStatus status;
//     TaskType type;
//     std::string filename;
//     TaskOutput outputs;
//     std::chrono::system_clock::time_point createTime;
//     std::chrono::system_clock::time_point updateTime;
    
// };
#endif
