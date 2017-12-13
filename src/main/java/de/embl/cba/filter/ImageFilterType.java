package de.embl.cba.filter;

public enum ImageFilterType
{
    THRESHOLD( "Threshold" ),
    GAUSS( "Gauss" ),
    DOG( "DoG"),
    DOG_THRESHOLD("DoG -> Threshold");

    private final String name;

    private ImageFilterType( final String name )
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }
}
