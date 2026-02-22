#pragma once

#include "Particle.h"
#include <cstddef>
#include <vector>

namespace atom {

/**
 * Physics simulation engine: Verlet integration + Coulomb attraction.
 * Electrons are attracted to the nucleus (center of mass of nucleons).
 * No graphics or network dependencies.
 */
class SimulationEngine {
public:
    using ParticleVec = std::vector<Particle>;

    /** Coulomb constant k in F = k * q1*q2/r^2. Tune for numerical stability. */
    double coulombConstant{1.0};

    /** Minimum distance for force calculation to avoid singularity. */
    double minDistance{1e-6};

    /** Target distance between nucleus particles (distance constraints). */
    double restDistance{1.0};

    /** Number of constraint solver passes per step (more = stiffer nucleus). */
    int constraintIterations{3};

    /** All particles (nucleus + electrons). */
    ParticleVec& particles() { return particles_; }
    const ParticleVec& particles() const { return particles_; }

    /** Add a particle; returns index. */
    size_t addParticle(Particle p);

    /** Single Verlet step: compute forces, then integrate, then apply constraints. */
    void step(double dt);

    /**
     * Distance constraints for all nucleus particle pairs.
     * Keeps each pair at restDistance (spring-like, position-based).
     */
    void applyConstraints();

    /**
     * Apply tiny random force to each nucleus particle (organic jitter).
     * Called each step; intensity scales the random acceleration.
     */
    void applyJitter(double intensity);

private:
    ParticleVec particles_;
    double jitterIntensity_{0.0};

    /** Force on particle at index i from Coulomb interaction with nucleus. */
    Vec3 coulombForceTowardNucleus(size_t i) const;

    /** Center of mass of all nucleus-type particles. */
    Vec3 nucleusCenter() const;

    /** Indices of particles with type Nucleus. */
    std::vector<size_t> nucleusIndices() const;
};

}  // namespace atom
