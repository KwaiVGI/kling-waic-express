#pragma once
#define _WINSOCKAPI_
#include <windows.h>
#include <DbgHelp.h>


#pragma comment(lib, "Dbghelp.lib")

// 崩溃回调
LONG WINAPI TopLevelExceptionHandler(PEXCEPTION_POINTERS pExceptionInfo);