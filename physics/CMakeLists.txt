cmake_minimum_required(VERSION 3.16)
project(AtomPhysics LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)

add_library(AtomPhysics
    Particle.cpp
    SimulationEngine.cpp
)
target_include_directories(AtomPhysics PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
