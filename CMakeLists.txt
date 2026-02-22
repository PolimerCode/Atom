cmake_minimum_required(VERSION 3.16)
project(Atom LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)

# WebSocket server (minimal deps: no TLS, no zlib)
set(USE_TLS OFF CACHE BOOL "Disable TLS for ixwebsocket" FORCE)
set(USE_ZLIB OFF CACHE BOOL "Disable zlib for ixwebsocket" FORCE)
include(FetchContent)
FetchContent_Declare(
    ixwebsocket
    GIT_REPOSITORY https://github.com/machinezone/IXWebSocket
    GIT_TAG        v11.4.6
)
FetchContent_MakeAvailable(ixwebsocket)

add_subdirectory(physics)
add_subdirectory(network)

add_executable(atom_main main.cpp)
target_link_libraries(atom_main PRIVATE AtomPhysics AtomNetwork)
