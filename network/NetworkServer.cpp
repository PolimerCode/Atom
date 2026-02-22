#include "NetworkServer.h"
#include "physics/Particle.h"
#include <ixwebsocket/IXNetSystem.h>
#include <ixwebsocket/IXWebSocketServer.h>
#include <sstream>
#include <thread>

namespace atom {

namespace {

const char* typeChar(ParticleType t) {
    return (t == ParticleType::Nucleus) ? "n" : "e";
}

}  // namespace

std::string NetworkServer::buildParticlesJson(const SimulationEngine::ParticleVec& particles) {
    std::ostringstream out;
    out << "[";
    for (size_t i = 0; i < particles.size(); ++i) {
        if (i) out << ",";
        const auto& p = particles[i];
        out << "{\"id\":" << p.id
            << ",\"t\":\"" << typeChar(p.type) << "\""
            << ",\"x\":" << p.position.x
            << ",\"y\":" << p.position.y
            << ",\"z\":" << p.position.z << "}";
    }
    out << "]";
    return out.str();
}

NetworkServer::NetworkServer(int port) : port_(port) {}

NetworkServer::~NetworkServer() {
    stop();
}

void NetworkServer::setSimulation(SimulationEngine* engine, std::mutex* engineMutex) {
    engine_ = engine;
    engineMutex_ = engineMutex;
}

void NetworkServer::start() {
    if (!engine_ || !engineMutex_) return;
    if (running_.exchange(true)) return;

    ix::initNetSystem();

    server_ = std::make_unique<ix::WebSocketServer>(port_, "0.0.0.0");
    server_->setOnConnectionCallback(
        [](std::weak_ptr<ix::WebSocket> /*ws*/, std::shared_ptr<ix::ConnectionState> /*state*/) {
            // Optional: log new connection
        });

    serverThread_ = std::thread(&NetworkServer::runServerThread, this);
    broadcastThread_ = std::thread(&NetworkServer::runBroadcastThread, this);
}

void NetworkServer::stop() {
    if (!running_.exchange(false)) return;

    if (server_) server_->stop();

    if (serverThread_.joinable()) serverThread_.join();
    if (broadcastThread_.joinable()) broadcastThread_.join();

    server_.reset();
    ix::uninitNetSystem();
}

void NetworkServer::runServerThread() {
    if (!server_) return;
    server_->listenAndStart();
}

void NetworkServer::runBroadcastThread() {
    const auto interval = std::chrono::milliseconds(broadcastIntervalMs_);

    while (running_) {
        std::this_thread::sleep_for(interval);
        if (!running_) break;

        std::string json;
        {
            std::lock_guard<std::mutex> lock(*engineMutex_);
            if (engine_)
                json = buildParticlesJson(engine_->particles());
        }

        if (json.empty() || !server_) continue;

        auto clients = server_->getClients();
        for (auto& client : clients) {
            if (client) client->sendText(json);
        }
    }
}

}  // namespace atom
