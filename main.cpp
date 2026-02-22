/**
 * Atomic simulation + WebSocket server.
 * Simulation runs continuously; server in a separate thread broadcasts particle state at 50 FPS.
 */
#include "network/NetworkServer.h"
#include "physics/SimulationEngine.h"
#include <chrono>
#include <cmath>
#include <cstdio>
#include <iostream>
#include <mutex>
#include <thread>

namespace {

/** Regular tetrahedron vertices (edge length 2*sqrt(2)); scale to get restDistance. */
atom::Vec3 tetrahedronVertex(size_t i) {
    const double raw[4][3] = {
        { 1,  1,  1},
        { 1, -1, -1},
        {-1,  1, -1},
        {-1, -1,  1}
    };
    return atom::Vec3(raw[i][0], raw[i][1], raw[i][2]);
}

}  // namespace

int main() {
    const double restDist = 1.0;
    const double scale = restDist / (2.0 * std::sqrt(2.0));

    atom::SimulationEngine engine;
    engine.restDistance = restDist;
    engine.coulombConstant = 1.2;
    engine.minDistance = 2.0;
    engine.constraintIterations = 4;

    for (size_t i = 0; i < 4; ++i) {
        atom::Vec3 pos = tetrahedronVertex(i) * scale;
        engine.addParticle(atom::Particle(
            pos, 1.0, 1.0, atom::ParticleType::Nucleus, static_cast<int>(i + 1)
        ));
    }

    const double orbitRadius = 5.0;
    const double dt = 0.008;
    const double electronMass = 0.02;
    const double tangentialSpeed = 6.5;
    atom::Particle electron(
        atom::Vec3(orbitRadius, 0.0, 0.0), electronMass, -1.0, atom::ParticleType::Electron, 5
    );
    electron.setVelocity(atom::Vec3(0.0, tangentialSpeed, 0.0), dt);
    engine.addParticle(std::move(electron));

    std::mutex engineMutex;
    atom::NetworkServer server(8080);
    server.setSimulation(&engine, &engineMutex);
    server.setBroadcastIntervalMs(20);  // 50 FPS
    server.start();

    std::cout << "WebSocket server on ws://0.0.0.0:8080 (50 FPS). Simulation running...\n";
    std::printf("step,pid,type,x,y,z\n");

    const int logSteps = 100;
    const double simSeconds = 30.0;
    const int totalSteps = static_cast<int>(simSeconds / dt);
    auto t0 = std::chrono::steady_clock::now();

    for (int step = 0; step < totalSteps; ++step) {
        {
            std::lock_guard<std::mutex> lock(engineMutex);
            engine.step(dt);

            if (step < logSteps) {
                for (size_t i = 0; i < engine.particles().size(); ++i) {
                    const auto& p = engine.particles()[i];
                    const char* typeStr = (p.type == atom::ParticleType::Nucleus) ? "nucleus" : "electron";
                    std::printf("%d,%d,%s,%.6f,%.6f,%.6f\n",
                        step + 1, p.id, typeStr,
                        p.position.x, p.position.y, p.position.z);
                }
            }
        }

        std::this_thread::sleep_until(t0 + std::chrono::duration<double>(dt * (step + 1)));
    }

    server.stop();
    std::cout << "Done.\n";
    return 0;
}
