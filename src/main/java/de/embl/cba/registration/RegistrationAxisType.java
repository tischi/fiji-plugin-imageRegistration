package de.embl.cba.registration;

import de.embl.cba.registration.util.Enums;

import java.util.ArrayList;
import java.util.List;

public enum RegistrationAxisType
{
    Sequence,
    Registration,
    Other;

    public static List< String > asStringList()
    {
        return Enums.asStringList( values() );
    }
}
