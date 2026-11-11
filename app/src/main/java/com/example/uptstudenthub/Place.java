package com.example.uptstudenthub;

import androidx.annotation.StringRes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Local com coordenadas (modelo imutável). O nome é um recurso de texto, sem Context. */
public final class Place {

    /** Porto, Lisboa e Faro (Anexo A, V1). */
    public static final List<Place> ALL = Collections.unmodifiableList(Arrays.asList(
            new Place("porto", R.string.ex_place_porto, 41.15, -8.61),
            new Place("lisboa", R.string.ex_place_lisboa, 38.72, -9.14),
            new Place("faro", R.string.ex_place_faro, 37.02, -7.93)));

    /** Identificador estável (usado nas chaves de favoritos). */
    public final String id;
    /** Recurso de texto com o nome do local. */
    @StringRes
    public final int nameRes;
    /** Latitude em graus. */
    public final double latitude;
    /** Longitude em graus. */
    public final double longitude;

    private Place(String id, int nameRes, double latitude, double longitude) {
        this.id = id;
        this.nameRes = nameRes;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
