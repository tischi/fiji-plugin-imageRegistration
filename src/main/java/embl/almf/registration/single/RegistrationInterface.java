package embl.almf.registration.single;

import embl.almf.image.ImageInterface;

public interface RegistrationInterface {
	
	public ImageInterface applyTransformation(ImageInterface image, Transformation transformation);
	
	Transformation computeTransformation(ImageInterface fixed, ImageInterface moving);

}
