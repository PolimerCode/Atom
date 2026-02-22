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
#include <random>
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

/** Random 3D unit vector (uniform on sphere). */
atom::Vec3 randomUnit3D(std::mt19937& rng) {
    std::uniform_real_distribution<double> u(-1.0, 1.0);
    atom::Vec3 v(u(rng), u(rng), u(rng));
    double len = v.length();
    if (len < 1e-9) return atom::Vec3(1, 0, 0);
    return v * (1.0 / len);
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
    engine.applyJitter(0.008);  // organic nucleus jitter

    for (size_t i = 0; i < 4; ++i) {
        atom::Vec3 pos = tetrahedronVertex(i) * scale;
        engine.addParticle(atom::Particle(
            pos, 1.0, 1.0, atom::ParticleType::Nucleus, static_cast<int>(i + 1)
        ));
    }

    const double dt = 0.008;
    const double electronMass = 0.02;
    std::mt19937 rng(std::random_device{}());

    // Electron 1: orbit ~r=5, XY plane
    atom::Particle e1(atom::Vec3(5.0, 0.0, 0.0), electronMass, -1.0, atom::ParticleType::Electron, 5);
    e1.setVelocity(atom::Vec3(0.0, 6.5, 0.0), dt);
    engine.addParticle(std::move(e1));

    // Electron 2: r=7, random 3D velocity (different plane)
    atom::Vec3 pos2(7.0, 0.0, 0.0);
    atom::Vec3 vel2 = randomUnit3D(rng) * 5.0;  // tangential-ish, orbit ~r=7
    atom::Particle e2(pos2, electronMass, -1.0, atom::ParticleType::Electron, 6);
    e2.setVelocity(vel2, dt);
    engine.addParticle(std::move(e2));

    // Electron 3: r=10, random 3D velocity (another plane)
    atom::Vec3 pos3(10.0, 0.0, 0.0);
    atom::Vec3 vel3 = randomUnit3D(rng) * 4.0;  // orbit ~r=10
    atom::Particle e3(pos3, electronMass, -1.0, atom::ParticleType::Electron, 7);
    e3.setVelocity(vel3, dt);
    engine.addParticle(std::move(e3));

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
