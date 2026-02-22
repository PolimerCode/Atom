cmake_minimum_required(VERSION 3.16)

add_library(AtomNetwork
    NetworkServer.cpp
)
target_include_directories(AtomNetwork PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_SOURCE_DIR}
)
target_link_libraries(AtomNetwork PUBLIC AtomPhysics ixwebsocket)
