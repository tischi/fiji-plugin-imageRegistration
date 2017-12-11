package embl.almf;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum RegistrationAxisType {

    SEQUENCE_DIMENSION( "Sequence" ),
    TRANSFORMABLE_DIMENSION( "Transformable" ),
    FIXED_DIMENSION( "Fixed" );

    private final String name;

    private RegistrationAxisType( final String name )
    {
        this.name = name;
    }

    public String toString()
    {
        return name;
    }

    public static List< String > asStringList()
    {
        List<String> enumNames = new ArrayList<>(  );

        for (RegistrationAxisType registrationAxisType : values() )
        {
            enumNames.add( registrationAxisType.toString() );
        }

        return enumNames;
    }

}
