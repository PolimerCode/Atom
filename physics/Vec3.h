#pragma once

#include <cmath>

namespace atom {

/**
 * Portable 3D vector for physics calculations.
 * No dependency on graphics or rendering libraries.
 */
struct Vec3 {
    double x{0.0};
    double y{0.0};
    double z{0.0};

    constexpr Vec3() = default;
    constexpr Vec3(double x_, double y_, double z_) : x(x_), y(y_), z(z_) {}

    Vec3& operator+=(const Vec3& o) {
        x += o.x;
        y += o.y;
        z += o.z;
        return *this;
    }

    Vec3& operator-=(const Vec3& o) {
        x -= o.x;
        y -= o.y;
        z -= o.z;
        return *this;
    }

    Vec3& operator*=(double s) {
        x *= s;
        y *= s;
        z *= s;
        return *this;
    }

    Vec3 operator+(const Vec3& o) const { return Vec3(x + o.x, y + o.y, z + o.z); }
    Vec3 operator-(const Vec3& o) const { return Vec3(x - o.x, y - o.y, z - o.z); }
    Vec3 operator*(double s) const { return Vec3(x * s, y * s, z * s); }
    Vec3 operator-() const { return Vec3(-x, -y, -z); }

    /** Squared length (avoids sqrt when comparing distances). */
    double lengthSq() const { return x * x + y * y + z * z; }

    double length() const { return std::sqrt(lengthSq()); }

    /** Normalized direction; returns zero vector if length is zero. */
    Vec3 normalized() const {
        double len = length();
        if (len <= 0.0) return Vec3(0, 0, 0);
        return *this * (1.0 / len);
    }

    /** Dot product. */
    double dot(const Vec3& o) const { return x * o.x + y * o.y + z * o.z; }
};

inline Vec3 operator*(double s, const Vec3& v) { return v * s; }

}  // namespace atom
