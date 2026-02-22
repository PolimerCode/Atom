#include "SimulationEngine.h"

namespace atom {

size_t SimulationEngine::addParticle(Particle p) {
    if (p.id == 0 && !particles_.empty()) {
        int maxId = 0;
        for (const auto& q : particles_)
            if (q.id > maxId) maxId = q.id;
        p.id = maxId + 1;
    }
    particles_.push_back(std::move(p));
    return particles_.size() - 1;
}

Vec3 SimulationEngine::nucleusCenter() const {
    Vec3 sum(0, 0, 0);
    double totalMass = 0.0;
    for (const auto& p : particles_) {
        if (p.type == ParticleType::Nucleus) {
            sum.x += p.position.x * p.mass;
            sum.y += p.position.y * p.mass;
            sum.z += p.position.z * p.mass;
            totalMass += p.mass;
        }
    }
    if (totalMass <= 0.0) return Vec3(0, 0, 0);
    return Vec3(sum.x / totalMass, sum.y / totalMass, sum.z / totalMass);
}

Vec3 SimulationEngine::coulombForceTowardNucleus(size_t i) const {
    const Particle& p = particles_[i];
    if (p.type != ParticleType::Electron) return Vec3(0, 0, 0);

    Vec3 center = nucleusCenter();
    Vec3 delta = center - p.position;
    double rSq = delta.lengthSq();
    double r = std::sqrt(rSq);
    if (r < minDistance) r = minDistance;
    rSq = r * r;

    // Total nucleus charge (sum of nucleon charges).
    double qNucleus = 0.0;
    for (const auto& n : particles_) {
        if (n.type == ParticleType::Nucleus) qNucleus += n.charge;
    }

    // F = k * |q1*q2| / r^2 toward nucleus (attraction when signs opposite).
    double magnitude = coulombConstant * std::abs(p.charge * qNucleus) / rSq;
    Vec3 direction = delta.normalized();
    return direction * magnitude;
}

void SimulationEngine::step(double dt) {
    if (dt <= 0.0) return;

    const double dtSq = dt * dt;

    for (size_t i = 0; i < particles_.size(); ++i) {
        Particle& p = particles_[i];

        Vec3 acceleration(0, 0, 0);
        if (p.type == ParticleType::Electron) {
            Vec3 force = coulombForceTowardNucleus(i);
            if (p.mass > 0.0)
                acceleration = force * (1.0 / p.mass);
        }
        // Nucleus particles: no force from this simple model (can add spring-mass later).

        // Verlet: x_new = 2*x - x_old + a*dt^2
        Vec3 newPosition = p.position * 2.0 - p.previous_position + acceleration * dtSq;

        p.previous_position = p.position;
        p.position = newPosition;
    }

    applyConstraints();
}

std::vector<size_t> SimulationEngine::nucleusIndices() const {
    std::vector<size_t> idx;
    for (size_t i = 0; i < particles_.size(); ++i) {
        if (particles_[i].type == ParticleType::Nucleus)
            idx.push_back(i);
    }
    return idx;
}

void SimulationEngine::applyConstraints() {
    std::vector<size_t> nuc = nucleusIndices();
    if (nuc.size() < 2) return;

    const double eps = 1e-9;

    for (int iter = 0; iter < constraintIterations; ++iter) {
        for (size_t a = 0; a < nuc.size(); ++a) {
            for (size_t b = a + 1; b < nuc.size(); ++b) {
                size_t i = nuc[a];
                size_t j = nuc[b];
                Particle& p1 = particles_[i];
                Particle& p2 = particles_[j];

                Vec3 delta = p2.position - p1.position;
                double d = delta.length();
                if (d < eps) continue;

                double diff = (d - restDistance) / d;
                Vec3 correction = delta * diff;

                double w1 = p1.mass;
                double w2 = p2.mass;
                double total = w1 + w2;
                if (total <= 0.0) total = 1.0;
                p1.position += correction * (w2 / total);
                p2.position -= correction * (w1 / total);
            }
        }
    }
}

}  // namespace atom
