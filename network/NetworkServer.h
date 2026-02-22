#pragma once

#include "physics/SimulationEngine.h"
#include <atomic>
#include <chrono>
#include <memory>
#include <mutex>
#include <string>
#include <thread>

namespace ix { class WebSocketServer; }

namespace atom {

/**
 * WebSocket server that broadcasts particle state at 50 FPS (every 20 ms).
 * Runs in its own thread; reads from SimulationEngine under mutex.
 * Decoupled from physics — no dependency from SimulationEngine to network.
 */
class NetworkServer {
public:
    /** Port to listen on (e.g. 8080). */
    explicit NetworkServer(int port = 8080);
    ~NetworkServer();

    NetworkServer(const NetworkServer&) = delete;
    NetworkServer& operator=(const NetworkServer&) = delete;

    /**
     * Bind to simulation state. Call before start().
     * engine and engineMutex must outlive this server.
     */
    void setSimulation(SimulationEngine* engine, std::mutex* engineMutex);

    /** Start listening and broadcast thread. Call after setSimulation(). */
    void start();

    /** Stop server and broadcast thread. */
    void stop();

    /** Broadcast interval in milliseconds (default 20 = 50 FPS). */
    void setBroadcastIntervalMs(int ms) { broadcastIntervalMs_ = ms; }

    int broadcastIntervalMs() const { return broadcastIntervalMs_; }

private:
    void runServerThread();
    void runBroadcastThread();

    /** Build JSON array: [{"id":1,"t":"n","x":0.1,"y":1.2,"z":-0.5}, ...] */
    static std::string buildParticlesJson(const SimulationEngine::ParticleVec& particles);

    int port_;
    int broadcastIntervalMs_{20};
    SimulationEngine* engine_{nullptr};
    std::mutex* engineMutex_{nullptr};

    std::unique_ptr<ix::WebSocketServer> server_;
    std::atomic<bool> running_{false};
    std::thread serverThread_;
    std::thread broadcastThread_;
};

}  // namespace atom
