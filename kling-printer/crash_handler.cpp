#include "crash_handler.h"
#include <ctime>
#include <iostream>
#include <sstream>

LONG WINAPI TopLevelExceptionHandler(PEXCEPTION_POINTERS pExceptionInfo)
{
    // 1. 构建文件名
    char dumpPath[MAX_PATH] = {};
    char logPath[MAX_PATH]  = {};
    time_t now = time(nullptr);
    tm t;
    localtime_s(&t, &now);
    sprintf_s(dumpPath, "crash_%04d%02d%02d_%02d%02d%02d.dmp",
              t.tm_year + 1900, t.tm_mon + 1, t.tm_mday,
              t.tm_hour, t.tm_min, t.tm_sec);
    sprintf_s(logPath, "crash_%04d%02d%02d_%02d%02d%02d.log",
              t.tm_year + 1900, t.tm_mon + 1, t.tm_mday,
              t.tm_hour, t.tm_min, t.tm_sec);

    // 2. 写 minidump
    HANDLE hFile = CreateFileA(dumpPath,
                               GENERIC_WRITE,
                               0, nullptr,
                               CREATE_ALWAYS,
                               FILE_ATTRIBUTE_NORMAL,
                               nullptr);
    if (hFile != INVALID_HANDLE_VALUE)
    {
        MINIDUMP_EXCEPTION_INFORMATION mei = {};
        mei.ThreadId          = GetCurrentThreadId();
        mei.ExceptionPointers = pExceptionInfo;
        mei.ClientPointers    = FALSE;
        MiniDumpWriteDump(GetCurrentProcess(),
                          GetCurrentProcessId(),
                          hFile,
                          MiniDumpNormal,
                          &mei,
                          nullptr,
                          nullptr);
        CloseHandle(hFile);
    }

    // 3. 打印简单日志
    FILE* log = nullptr;
    fopen_s(&log, logPath, "w");
    if (log)
    {
        fprintf(log, "Exception Code : 0x%08X\n", pExceptionInfo->ExceptionRecord->ExceptionCode);
        fprintf(log, "Exception Addr : 0x%p\n", pExceptionInfo->ExceptionRecord->ExceptionAddress);
#ifdef _WIN64
        fprintf(log, "RIP = 0x%016llX\n", pExceptionInfo->ContextRecord->Rip);
#else
        fprintf(log, "EIP = 0x%08X\n", pExceptionInfo->ContextRecord->Eip);
#endif
        fclose(log);
    }

    // 4. 返回继续默认处理（弹系统框）
    return EXCEPTION_EXECUTE_HANDLER;
}