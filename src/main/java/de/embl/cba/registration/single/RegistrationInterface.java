package de.embl.cba.registration.single;

import de.embl.cba.image.ImageInterface;

public interface RegistrationInterface {
	
	public ImageInterface applyTransformation(ImageInterface image, Transformation transformation);
	
	Transformation computeTransformation(ImageInterface fixed, ImageInterface moving);

}
