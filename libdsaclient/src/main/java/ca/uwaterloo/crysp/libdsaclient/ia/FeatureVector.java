package ca.uwaterloo.crysp.libdsaclient.ia;

import java.util.Arrays;


/**
 * A vector of features
 * 
 * @author Aaron Atwater
 * @author Hassan Khan
 *
 */
public class FeatureVector {
	/**
	 * A sparse array to store the features
	 */
	//public ArrayList<Double> features;
	protected double[] features;
	
	/**
	 * Label of the class i.e. positive instance or negative instance
	 * 
	 */
	private int classLabel;
	//private double[] values;
	//private int classAtt = 0;
	
	/**
	 * Copy constructor for FeatureVector class
	 * @param _fv Uses this FeatureVector to generate a copy constructor
	 */
	public FeatureVector(FeatureVector _fv) {
		this.features = new double[_fv.features.length];
		for(int i = 0; i <_fv.features.length; i++)
			set(i, _fv.get(i));
		this.classLabel = _fv.classLabel;
	}

	/**
	 * Constructs FeatureVector with a capacity of numFeatures features
	 * @param numFeatures Capacity of the FeatureVector
	 */
	public FeatureVector(int numFeatures) {
		this.features = new double[numFeatures]; 
	}
	
	
	/**
	 * Creates a FeatureVector using the feature values in 'vals' & classlabel
	 * in '_classlabel'
	 * @param vals Double array containing the feature score
	 * @param _classLabel class Label of the FeatureVector
	 */
	public FeatureVector(double [] vals, int _classLabel) {
		this.features = new double[vals.length];
		for (int i = 0; i < vals.length; i++)
			this.set(i, vals[i]);
		this.classLabel = _classLabel;
	}

	/**
	 * Returns size of the FeatureVector
	 * @return size of the FeatureVector
	 */
	public int size() {
		return this.features.length;
	}

	/**
	 * Returns class label of this FeatureVector
	 * @return Class Label of this FeatureVector
	 */
	public int getClassLabel() {
		return classLabel;
	}

	/**
	 * Sets class label of this FeatureVector
	 * @param classLabel class label to set
	 */
	public void setClassLabel(int classLabel) {
		this.classLabel = classLabel;
	}
	
	/**
	 * Sets the value at a particular index of the FeatureVector
	 * @param index Index of the FeatureVector to modify
	 * @param value New value to set at the index
	 */
	public void set(int index, double value) {
		if (index < 0 || index >= features.length) return; //TODO: fail harder
		features[index] = value;
	}
	
	/**
	 * Get the value of the feature at a specific index.
	 * 
	 * @param index index of the attribute to return
	 * @return the value of the feature at <tt>index</tt>
	 */
	public double get(int index) {
		if (index < 0 || index >= features.length) return 0.0; //TODO: fail harder
		return features[index];
	}
	
	/**
	 * Gets the feature score at the index 'index'; if n value exists, returns
	 * the default '_default'
	 * @param index index of the FeatureVector to get the feature score from
	 * @param _default default value to return if sparse array index is not
	 * populated
	 * @return returns the feature score if index exists or default otherwise
	 */
	public double get(int index, double _default) {
		if (index < 0 || index >= features.length) return _default; //TODO: fail harder
		return features[index];
	}
	
	/**
	 * Get the feature values as an array
	 * 
	 * @return a double array containing a *copy* of all feature values
	 */
	public double[] getAll() {
		return Arrays.copyOf(features, features.length);
	}

	/**
	 * Reset all feature values to zero.
	 */
	public void clear() {
		for (int i=0; i<features.length; i++)
			features[i] = 0;
	}
}

