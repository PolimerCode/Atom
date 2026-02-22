/**
 * Test: tetrahedron nucleus (4 particles) + 1 electron on orbit.
 * Prints positions for the first 100 steps to verify stability and orbit.
 */
#include "physics/SimulationEngine.h"
#include <cmath>
#include <cstdio>
#include <iostream>

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
    const double scale = restDist / (2.0 * std::sqrt(2.0));  // so edges = restDist

    atom::SimulationEngine engine;
    engine.restDistance = restDist;
    engine.coulombConstant = 1.2;
    engine.minDistance = 2.0;  // effective "core" radius so orbit stays stable
    engine.constraintIterations = 4;

    // Nucleus: 4 particles in tetrahedron
    for (size_t i = 0; i < 4; ++i) {
        atom::Vec3 pos = tetrahedronVertex(i) * scale;
        engine.addParticle(atom::Particle(
            pos, 1.0, 1.0, atom::ParticleType::Nucleus, static_cast<int>(i + 1)
        ));
    }

    // Electron: start at orbitRadius, tangential velocity for stable orbit
    // Approx orbital v: sqrt(coulombConstant * |q_nucleus| / (mass * r)) with tuned mass
    const double orbitRadius = 5.0;
    const double dt = 0.008;
    const double electronMass = 0.02;
    const double tangentialSpeed = 6.5;  // ~circular orbit: v^2/r = k*Q/(m*r) => v ≈ sqrt(48) ≈ 6.9 at r=5
    atom::Particle electron(
        atom::Vec3(orbitRadius, 0.0, 0.0), electronMass, -1.0, atom::ParticleType::Electron, 5
    );
    electron.setVelocity(atom::Vec3(0.0, tangentialSpeed, 0.0), dt);
    engine.addParticle(std::move(electron));

    std::printf("step,pid,type,x,y,z\n");

    const int logSteps = 100;
    for (int step = 0; step < logSteps; ++step) {
        engine.step(dt);

        for (size_t i = 0; i < engine.particles().size(); ++i) {
            const auto& p = engine.particles()[i];
            const char* typeStr = (p.type == atom::ParticleType::Nucleus) ? "nucleus" : "electron";
            std::printf("%d,%d,%s,%.6f,%.6f,%.6f\n",
                step + 1, p.id, typeStr,
                p.position.x, p.position.y, p.position.z);
        }
    }

    std::cout << "Done. First " << logSteps << " steps printed (step,pid,type,x,y,z).\n";
    return 0;
}
