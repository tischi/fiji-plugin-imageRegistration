package de.embl.cba.registration.utils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Enums
{

    public static List< String > asStringList( Enum[] enums )
    {
        return Stream.of( enums )
                .map( Enum::name )
                .collect( Collectors.toList() );
    }
}
