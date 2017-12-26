package de.embl.cba.registration.ui;

import de.embl.cba.registration.OutputIntervalType;
import de.embl.cba.registration.RegistrationAxisTypes;
import de.embl.cba.registration.filter.ImageFilterParameters;
import de.embl.cba.registration.filter.ImageFilterType;
import de.embl.cba.registration.transformationfinders.TransformationFinderParameters;
import de.embl.cba.registration.transformationfinders.TransformationFinderType;
import net.imglib2.FinalInterval;
import net.imglib2.util.Intervals;
import org.scijava.module.Module;

import java.util.HashMap;
import java.util.Map;

class RegistrationParameters
{
    private RegistrationPlugin plugin;
    Module module;

    RegistrationAxisTypes[] registrationAxisTypes;
    Map< String, Object > imageFilterParameters;
    Map< String, Object > transformationParameters;
    OutputIntervalType outputIntervalType;
    FinalInterval interval;

    public RegistrationParameters( RegistrationPlugin plugin )
    {
        this.plugin = plugin;
        this.module = (Module) plugin;
        updateParameters();
    }

    public void updateParameters()
    {
        setRegistrationAxesTypes();
        setImageFilterParameters();
        setTransformationParameters();
        setRegistrationAxesInterval();
        setOutputInterval();
    }

    private void setRegistrationAxesTypes()
    {
        registrationAxisTypes = new RegistrationAxisTypes[ plugin.dataset.numDimensions() ];

        for ( int d = 0; d < plugin.dataset.numDimensions(); ++d )
        {
            registrationAxisTypes[ d ] = RegistrationAxisTypes.valueOf( plugin.typeInput( d ).getValue( this ) );
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

        String[] thresholdMinMax = imageFilterParameterThresholdInput.split( "," );
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
        return imageFilterParameters;
    }

    private void setTransformationParameters()
    {
        String[] tmp;

        transformationParameters = new HashMap<>();

        boolean showFixedImageSequence = true;

        transformationParameters.put(
                TransformationFinderParameters.TRANSFORMATION_FINDER_TYPE,
                TransformationFinderType.valueOf( plugin.transformationTypeInput ) );

        tmp = plugin.transformationParametersMaximalTranslationsInput.split( "," );
        double[] transformationParametersMaximalTranslations = new double[ tmp.length ];
        for ( int i = 0; i < tmp.length; ++i )
        {
            transformationParametersMaximalTranslations[ i ] = Double.parseDouble( tmp[ i ].trim() );
        }
        transformationParameters.put(
                TransformationFinderParameters.MAXIMAL_TRANSLATIONS,
                transformationParametersMaximalTranslations );


        tmp = plugin.transformationParameterMaximalRotationsInput.split( "," );
        double[] transformationParametersMaximalRotations = new double[ tmp.length ];
        for ( int i = 0; i < tmp.length; ++i )
        {
            transformationParametersMaximalRotations[ i ] = Double.parseDouble( tmp[ i ] );
        }
        transformationParameters.put(
                TransformationFinderParameters.MAXIMAL_ROTATIONS,
                transformationParametersMaximalRotations );

    }

    private void setOutputInterval()
    {
        outputIntervalType = OutputIntervalType.valueOf( plugin.outputViewIntervalSizeTypeInput );
    }
}