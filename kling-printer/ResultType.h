#include <string>
#include <vector>
#include <chrono>

enum class PrintingStatus {
    QUEUING,
    PRINTING,
    COMPLETED
};

struct Printing {
    long id;
    std::string name;
    Task task;
    PrintingStatus status;
};

struct PrintingUpdateInput {
    PrintingStatus status;
};

enum class TaskStatus {
    SUBMITTED,
    PROCESSING,
    SUCCEED,
    FAILED
};

enum class TaskOutputType {
    IMAGE,
    VIDEO
};
enum class TaskType : long long {
    STYLED_IMAGE = 100000,
    VIDEO_EFFECT = 500000
};

struct TaskOutput {
    TaskOutputType type;
    std::string url;
};

struct Task {
    long long id;
    std::string name;
    std::vector<std::string> taskIds;
    TaskStatus status;
    TaskType type;
    std::string filename;
    TaskOutput outputs;
    std::chrono::system_clock::time_point createTime;
    std::chrono::system_clock::time_point updateTime;
    
};
