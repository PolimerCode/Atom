#pragma once

#include "Vec3.h"
#include <cstdint>

namespace atom {

/** Particle type for simulation (nucleus vs electron). */
enum class ParticleType : uint8_t {
    Nucleus,  // proton/neutron
    Electron
};

/**
 * Base particle for Verlet integration.
 * Stores position and previous position; velocity is implicit (position - previous_position) / dt.
 * No graphics or rendering dependencies.
 */
class Particle {
public:
    Vec3 position;
    Vec3 previous_position;
    double mass{1.0};
    /** Electric charge (e.g. +1 for proton, -1 for electron in elementary charge units). */
    double charge{0.0};
    ParticleType type{ParticleType::Nucleus};
    /** Optional id for external sync (e.g. network). */
    int id{0};

    Particle() = default;

    Particle(const Vec3& pos, double mass_, double charge_, ParticleType type_, int id_ = 0)
        : position(pos)
        , previous_position(pos)
        , mass(mass_)
        , charge(charge_)
        , type(type_)
        , id(id_)
    {}

    /** Velocity from Verlet state: (position - previous_position) / dt. */
    Vec3 velocity(double dt) const {
        if (dt <= 0.0) return Vec3(0, 0, 0);
        return (position - previous_position) * (1.0 / dt);
    }

    /** Set position and previous_position so that effective velocity is v. */
    void setVelocity(const Vec3& v, double dt) {
        previous_position = position - v * dt;
    }
};

}  // namespace atom
