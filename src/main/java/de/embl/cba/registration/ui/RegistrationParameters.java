package de.embl.cba.registration.ui;

import de.embl.cba.registration.OutputIntervalType;
import de.embl.cba.registration.RegistrationAxisType;
import de.embl.cba.registration.filter.ImageFilterParameters;
import de.embl.cba.registration.filter.ImageFilterType;
import de.embl.cba.registration.transformfinder.TransformFinderParameters;
import de.embl.cba.registration.transformfinder.TransformationFinderType;
import net.imglib2.FinalInterval;
import net.imglib2.util.Intervals;
import org.scijava.module.Module;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class RegistrationParameters
{
    private final RegistrationPlugin plugin;
    private final Module module; // TODO: what is the difference between plugin and module?

    public RegistrationAxisType[] registrationAxisTypes;
    public Map< String, Object > imageFilterParameters;
    public Map< String, Object > transformParameters;
    public OutputIntervalType outputIntervalType;
    public FinalInterval interval;
    public ExecutorService executorService;

    public RegistrationParameters( RegistrationPlugin plugin )
    {
        this.plugin = plugin;
        this.module = plugin;
        updateParameters();
    }

    public void updateParameters()
    {
        setRegistrationAxesTypes();
        setImageFilterParameters();
        setTransformationParameters();
        setRegistrationAxesInterval();
        setOutputInterval();
        setNumThreads();
    }

    private void setNumThreads()
    {
        executorService = plugin.threadService.getExecutorService(); // TODO
    }

    private void setRegistrationAxesTypes()
    {
        registrationAxisTypes = new RegistrationAxisType[ plugin.dataset.numDimensions() ];

        for ( int d = 0; d < plugin.dataset.numDimensions(); ++d )
        {
            String axisTypeName = ( String ) plugin.typeInput( d ).getValue( module );
            registrationAxisTypes[ d ] = RegistrationAxisType.valueOf( axisTypeName );
        }

    }

    private void setRegistrationAxesInterval()
    {
        long[] min = Intervals.minAsLongArray( plugin.dataset );
        long[] max = Intervals.maxAsLongArray( plugin.dataset );

        for ( int d = 0; d < plugin.dataset.numDimensions(); ++d )
        {
            min[ d ] = (long) plugin.varInput( d, "min" ).getValue( module );
            max[ d ] = (long) plugin.varInput( d, "max" ).getValue( module );
        }

        interval = new FinalInterval( min, max );
    }

    private void setImageFilterParameters()
    {
        imageFilterParameters = new HashMap<>();

        ImageFilterType imageFilterType = ImageFilterType.valueOf( plugin.imageFilterTypeInput );

        imageFilterParameters.put(
                ImageFilterParameters.FILTER_TYPE,
                imageFilterType );

        String[] thresholdMinMax = plugin.imageFilterParameterThresholdInput.split( "," );
        imageFilterParameters.put(
                ImageFilterParameters.THRESHOLD_MIN_VALUE,
                Double.parseDouble( thresholdMinMax[ 0 ].trim() ) );
        imageFilterParameters.put(
                ImageFilterParameters.THRESHOLD_MAX_VALUE,
                Double.parseDouble( thresholdMinMax[ 1 ].trim() ) );


        // below are currently hard coded and cannot be changed from the GUI
        imageFilterParameters.put(
                ImageFilterParameters.DOG_SIGMA_SMALLER, new double[]{ 2.0D, 2.0D } );
        imageFilterParameters.put(
                ImageFilterParameters.DOG_SIGMA_LARGER, new double[]{ 5.0D, 5.0D } );
        imageFilterParameters.put(
                ImageFilterParameters.GAUSS_SIGMA, new double[]{ 10.0D, 1.0D } );

    }

    private void setTransformationParameters()
    {
        String[] tmp;

        transformParameters = new HashMap<>();

        boolean showFixedImageSequence = true;

        transformParameters.put(
                TransformFinderParameters.TRANSFORMATION_FINDER_TYPE,
                TransformationFinderType.valueOf( plugin.transformationTypeInput ) );

        tmp = plugin.transformationParametersMaximalTranslationsInput.split( "," );
        double[] transformationParametersMaximalTranslations = new double[ tmp.length ];
        for ( int i = 0; i < tmp.length; ++i )
        {
            transformationParametersMaximalTranslations[ i ] = Double.parseDouble( tmp[ i ].trim() );
        }
        transformParameters.put(
                TransformFinderParameters.MAXIMAL_TRANSLATIONS,
                transformationParametersMaximalTranslations );


        tmp = plugin.transformationParameterMaximalRotationsInput.split( "," );
        double[] transformationParametersMaximalRotations = new double[ tmp.length ];
        for ( int i = 0; i < tmp.length; ++i )
        {
            transformationParametersMaximalRotations[ i ] = Double.parseDouble( tmp[ i ] );
        }
        transformParameters.put(
                TransformFinderParameters.MAXIMAL_ROTATIONS,
                transformationParametersMaximalRotations );

    }

    private void setOutputInterval()
    {
        outputIntervalType = OutputIntervalType.valueOf( plugin.outputViewIntervalSizeTypeInput );
    }
}