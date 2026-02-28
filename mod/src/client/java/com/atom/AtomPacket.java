package com.atom;

import com.google.gson.annotations.SerializedName;

/**
 * Пакет частицы из C++ сервера. JSON: {"id":1,"t":"n","x":0.1,"y":1.2,"z":-0.5}
 */
public class AtomPacket {
    public int id;
    @SerializedName("t")
    public String type;
    public double x;
    public double y;
    public double z;

    public boolean isNucleus() {
        return "n".equals(type);
    }

    public boolean isElectron() {
        return "e".equals(type);
    }
}
