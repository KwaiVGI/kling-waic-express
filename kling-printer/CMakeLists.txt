# CMakeLists.txt
cmake_minimum_required(VERSION 3.16)
project(kling-printer)

# 设置C++标准
set(CMAKE_CXX_STANDARD 17)

# 设置版本号
set(VERSION_MAJOR 1)
set(VERSION_MINOR 0)

# 自动处理Qt元对象系统(MOC)、UI文件和资源文件
set(CMAKE_AUTOMOC ON)
set(CMAKE_AUTORCC ON)
set(CMAKE_AUTOUIC ON)
# 手动指定Qt路径（关键！）
set(CMAKE_PREFIX_PATH "C:\\Qt\\6.9.1\\msvc2022_64")
set(OPENSSL_ROOT_DIR "D:\\vcpkg\\packages\\openssl_x64-windows")
set(OPENSSL_INCLUDE_DIR "D:\\vcpkg\\packages\\openssl_x64-windows\\include")

find_package(Qt6 COMPONENTS 
    Core 
    Gui 
    Widgets
    WebEngineWidgets
    REQUIRED
)
find_package(OpenSSL REQUIRED)

add_executable(${PROJECT_NAME} WIN32
    main.cpp Printer.cpp Printer.h PrinterManager.h PrinterManager.cpp
    ConnectPool.h ConnectPool.cpp HttpClient.h HttpClient.cpp ResultType.h
    # 其他源文件...
)

target_link_libraries(${PROJECT_NAME} PRIVATE 
    Qt6::Core 
    Qt6::Gui 
    Qt6::Widgets
    Qt6::WebEngineWidgets
)
target_link_libraries(${PROJECT_NAME} PRIVATE
    OpenSSL::SSL
    OpenSSL::Crypto)
add_compile_definitions(CPPHTTPLIB_OPENSSL_SUPPORT)
# target_link_libraries(${PROJECT_NAME} PRIVATE ssl crypto pthread)