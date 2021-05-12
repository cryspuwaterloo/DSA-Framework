package ca.uwaterloo.crysp.libdsaclient.ia;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TrainingSet implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public List<double[]> fv;
	public double fScale [];
	
	public TrainingSet() {
		fv = new ArrayList<double[]>();
		fScale = new double[29];
	}

}
