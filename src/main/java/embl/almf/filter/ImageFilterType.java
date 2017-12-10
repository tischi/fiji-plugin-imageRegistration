package embl.almf.filter;

public enum ImageFilterType
{
    THRESHOLD( "Threshold" ),
    GAUSS( "Gauss" ),
    DOG( "Difference of gaussian");

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
