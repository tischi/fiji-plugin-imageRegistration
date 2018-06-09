package de.embl.cba.registration;

import de.embl.cba.registration.utils.Enums;

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
