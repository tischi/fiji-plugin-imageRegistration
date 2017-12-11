package embl.almf;

import java.util.ArrayList;

public enum TransformationAxisType {

    SEQUENCE_DIMENSION( "Sequence" ),
    TRANSFORMABLE_DIMENSION( "Transformable" ),
    FIXED_DIMENSION( "Fixed" );

    private final String name;

    private TransformationAxisType( final String name )
    {
        this.name = name;
    }

    public String toString()
    {
        return name;
    }

    public final static ArrayList< String > asStringList()
    {
        ArrayList<String> enumNames = new ArrayList<>(  );

        for (TransformationAxisType registrationAxisType : values() )
        {
            enumNames.add( registrationAxisType.toString() );
        }

        return enumNames;
    }

}
