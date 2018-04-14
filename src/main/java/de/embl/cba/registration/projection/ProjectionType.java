package de.embl.cba.registration.projection;

import de.embl.cba.registration.util.Enums;

import java.util.ArrayList;
import java.util.List;

public enum ProjectionType
{
    Average,
    Median,
    Maximum,
    Minimum;

    public static List< String > asStringList()
    {
        return Enums.asStringList( values() );
    }
}
