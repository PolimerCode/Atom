cmake_minimum_required(VERSION 3.16)
project(Atom LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)

add_subdirectory(physics)

add_executable(atom_main main.cpp)
target_link_libraries(atom_main PRIVATE AtomPhysics)
