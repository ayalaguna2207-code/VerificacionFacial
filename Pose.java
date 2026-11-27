package com.laura.verificacion;

/** Las tres poses que se piden al registrar el rostro. */
public enum Pose {
    FRENTE("Mira de frente a la cámara"),
    IZQUIERDA("Gira la cabeza hacia TU izquierda"),
    DERECHA("Gira la cabeza hacia TU derecha");

    public final String instruccion;

    Pose(String instruccion) {
        this.instruccion = instruccion;
    }
}
