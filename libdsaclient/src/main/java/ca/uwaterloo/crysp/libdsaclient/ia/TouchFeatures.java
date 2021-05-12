package ca.uwaterloo.crysp.libdsaclient.ia;

import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;


public class TouchFeatures {
	
	public FeatureVector fv;
	/**
	 * Touch Point class. stores the data retrieved from TouchEvent 
	 * @author Aaron Atwater
	 * @author Hassan Khan
	 */
	private class TouchPoint {
		long eventTimestamp;
		double xVal, yVal, pressure, width, orientation;
	}

	/**
	 * TouchPoints for the current swipe
	 */
	private List<TouchPoint> touchPoints = new ArrayList<TouchPoint>();

	/**
	 * time of last swipe
	 */
	protected static double lastSwipeTimestamp = 0;

	/**
	 * Number of features currently supported by this class
	 */
	public static final int NUM_FEATURES = 29;

	/**
	 * List of all the features supported
	 */
	protected String featureList [] = {
			"Start X",
			"Start Y",
			"End X",
			"End Y",
			"Duration in ms",
			"Inter-stroke time in ms",
			"Direct end-to-end distance",
			"Mean Resultant Length",
			"20% perc. pairwise velocity",
			"50% perc. pairwise velocity",
			"80% perc. pairwise velocity",
			"20% perc. pairwise acceleration",
			"50% perc. pairwise acceleration",
			"80% perc. pairwise acceleration",
			"Direction of End-to-End line",
			"Median velocity of last 3 points",
			"Length of Trajectory",
			"Average Velocity",
			"Median Acceleration at first 5 points",
			"Pressure in the middle of stroke",
			"Midstroke area covered",
			"Phone Orientation",
			"Direction Flag",
			"Ratio of Direct Distance and Traj. Length",
			"Average Direction of ensemble Pairs",
			"Largest Deviation from end-end Line",
			"20% perc. Deviation from end-to-end line",
			"50% perc. Deviation from end-to-end line",
			"80% perc. Deviation from end-to-end line"
	};
		
	/**
	 * Default constructor for Touch
	 */
	public TouchFeatures(){
		fv = new FeatureVector(29);
	}
	
	/* (non-Javadoc)
	 * @see ca.uwaterloo.crysp.itus.measurements.Measurement#procEvent(java.lang.Object, ca.uwaterloo.crysp.itus.measurements.EventType)
	 */
	public boolean procEvent(MotionEvent event) {
		int action = event.getAction();

		switch(action) {
		case MotionEvent.ACTION_DOWN:  /* primary pointer */
		case MotionEvent.ACTION_POINTER_DOWN: /* any subsequent pointer */
			/*No need for a swipe ID*/
			break;
		case MotionEvent.ACTION_MOVE: /* any number of pointers move */
			for (int hIndx = 0; hIndx < event.getHistorySize(); hIndx++) {
				for (int pIndex = 0; pIndex < event.getPointerCount(); 
						pIndex++) {
					TouchPoint tp = new TouchPoint();
					tp.xVal = event.getHistoricalX(pIndex, hIndx);
					tp.yVal = event.getHistoricalY(pIndex, hIndx);
					tp.pressure = event.getHistoricalPressure(pIndex, hIndx);
					tp.width = event.getHistoricalSize(pIndex, hIndx);
					tp.orientation = event.getHistoricalOrientation(pIndex, hIndx);
					tp.eventTimestamp = event.getHistoricalEventTime(hIndx);
					this.touchPoints.add(tp);
				}
			}

			for (int pIndex = 0; pIndex < event.getPointerCount(); 
					pIndex++) {
				TouchPoint tp = new TouchPoint();
				tp.xVal = event.getX(pIndex);
				tp.yVal = event.getY(pIndex);
				tp.pressure = event.getPressure(pIndex);
				tp.width = event.getSize(pIndex);
				tp.eventTimestamp = event.getEventTime();
				tp.orientation = event.getOrientation(pIndex);
				this.touchPoints.add(tp);
			}
			break;
		case MotionEvent.ACTION_POINTER_UP: /* all pointers are up */
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			/* XXX return if length of swipe is less than 6 touchpoints*/
			if (this.touchPoints.size() < 10) {
				this.touchPoints.clear();
				return false;
			} 
			else {
				this.populateFeatureVector();
				TouchFeatures.lastSwipeTimestamp = this.touchPoints.get(
						this.touchPoints.size() - 1).eventTimestamp;
				this.touchPoints.clear();
				return true;
			}
		}
		return false;
	}

		
	/**
	 * Returns the populated list of FeatureVectors
	 */
	private void populateFeatureVector() {
		/*since this is a live collection, set to positive instance?*/
		fv.setClassLabel(1);

		/*Feature List*/
		/**Start X (F1)*/
		/**Start Y (F2)*/
		/**End X (F3)*/
		/**End Y (F4)*/
		/**Duration in ms (F5)*/
		/**Inter-stroke time in ms (F6)*/
		/**Direct end-to-end distance (F7)*/
		/**Mean Resultant Length (F8)*/
		/**20% perc. pairwise velocity (F9)*/
		/**50% perc. pairwise velocity (F10)*/
		/**80% perc. pairwise velocity (F11)*/
		/**20% perc. pairwise acceleration (F12)*/
		/**50% perc. pairwise acceleration (F13)*/
		/**80% perc. pairwise acceleration (F14)*/
		/**Direction of End-to-End line (F15)*/
		/**Median velocity of last 3 points (F16)*/
		/**Length of Trajectory (F17)*/
		/**Average Velocity (F18)*/
		/**Median Acceleration at first 5 points (F19)*/
		/**Pressure in the middle of stroke (F20)*/
		/**Midstroke area covered (F21)*/
		/**Phone Orientation (F22)*/
		/**Direction Flag (F23)*/
		/**Ratio of Direct Distance and Traj. Length (F24)*/
		/**Average Direction of ensemble Pairs (F25)*/
		/**Largest Deviation from end-end Line (F26)*/
		/**20% perc. Deviation from end-to-end line (F27)*/
		/**50% perc. Deviation from end-to-end line (F28)*/
		/**80% perc. Deviation from end-to-end line (F29)*/

		int numPoints = this.touchPoints.size();
		/*F 1-7 are pretty straightforward*/
		fv.set(0, this.touchPoints.get(0).xVal); /*startX*/
		fv.set(1, this.touchPoints.get(0).yVal); /*startY*/
		fv.set(2, this.touchPoints.get(numPoints - 1).xVal); /*endX*/
		fv.set(3, this.touchPoints.get(numPoints - 1).yVal); /*endY*/
		fv.set(4, this.touchPoints.get(numPoints - 1).eventTimestamp - 
				this.touchPoints.get(0).eventTimestamp); /*duration*/
		fv.set(5,0);
		//fv.set(5, this.touchPoints.get(numPoints - 1).eventTimestamp -
		//		TouchFeatures.lastSwipeTimestamp); /*interStrokeTime*/
		fv.set(6, Math.sqrt(Math.pow(fv.get(2) - fv.get(0), 2) +
				Math.pow(fv.get(3) - fv.get(1), 2))); /*directDistance*/

		/*Calculate pairwise displacement, velocity and acceleration*/
		double xDisplacement[] = new double[numPoints - 1];
		double yDisplacement[] = new double[numPoints - 1];
		double tDisplacement[] = new double[numPoints - 1];
		double pairwAngle[] = new double[numPoints - 1];
		double pairwDistance[] = new double[numPoints - 1];
		double pairwVelocity[] = new double[numPoints - 1];
		double pairwAcceleration[] = new double[numPoints - 2];

		for (int i = 0; i < numPoints - 2; i++) {
			xDisplacement[i] = this.touchPoints.get(i+1).xVal - 
					this.touchPoints.get(i).xVal;
			yDisplacement[i] = this.touchPoints.get(i+1).yVal - 
					this.touchPoints.get(i).yVal;
			tDisplacement[i] = this.touchPoints.get(i+1).eventTimestamp - 
					this.touchPoints.get(i).eventTimestamp;
			pairwAngle[i] =  Math.atan2(yDisplacement[i], xDisplacement[i]);
			pairwDistance[i] =  Math.sqrt(Math.pow(xDisplacement[i], 2) +
					Math.pow(yDisplacement[i], 2));
			if (tDisplacement[i] == 0) 
				pairwVelocity[i] = 0;
			else 
				pairwVelocity[i] = pairwDistance[i]/tDisplacement[i];
		}
		/*correct pairwVelocity by setting '0' to maxVelocity*/
		double maxVelocity = ArrayUtil.max(pairwVelocity);
		for (int i = 0; i < pairwVelocity.length - 1; i++) 
			if (pairwVelocity[i] == 0)
				pairwVelocity[i] = maxVelocity;

		for (int i = 0; i < pairwVelocity.length - 2; i++) {
			pairwAcceleration[i] = pairwVelocity[i+1] - pairwVelocity[i];
			if (tDisplacement[i] == 0) 
				pairwAcceleration[i] = 0;
			else 
				pairwAcceleration[i] = pairwAcceleration[i]/tDisplacement[i];
		}
		/*calculate the max values for acceleration and replace
		 * values for which tDisplacement = 0 to max*/
		double maxAcceleration = 0;

		maxAcceleration = ArrayUtil.max(pairwAcceleration);

		for (int i = 0; i < pairwAcceleration.length - 1; i++) 
			if (pairwAcceleration[i] == 0)
				pairwAcceleration[i] = maxAcceleration;	

		/*F8-15*/
		fv.set(7, ComplexNumbers.circ_r(pairwAngle)); /*meanResultantLength*/
		fv.set(8, ArrayUtil.percentile(pairwVelocity, 0.20)); /*velocity20*/
		fv.set(9, ArrayUtil.percentile(pairwVelocity, 0.50)); /*velocity50*/
		fv.set(10, ArrayUtil.percentile(pairwVelocity, 0.80)); /*velocity80*/
		fv.set(11, ArrayUtil.percentile(pairwAcceleration, 0.20)); /*acceleration20*/
		fv.set(12, ArrayUtil.percentile(pairwAcceleration, 0.50)); /*acceleration50*/
		fv.set(13, ArrayUtil.percentile(pairwAcceleration, 0.80)); /*acceleration80*/
		fv.set(14, Math.atan2(fv.get(3) - fv.get(1),
				fv.get(2) - fv.get(0))); /*lineDirection*/

		/*F16 last 3 velocity points*/
		double velocityPoints [] = {pairwVelocity[pairwVelocity.length - 1],
				pairwVelocity[pairwVelocity.length-2],
				pairwVelocity[pairwVelocity.length-3]};
		fv.set(15, ArrayUtil.percentile(velocityPoints, 0.50)); /*medVelocity*/

		/*F17-18: trajectoryLength & averageVelocity*/
		double temp = 0;

		for (int i = 0; i < pairwDistance.length; i++) {
			temp += pairwDistance[i]; /*trajectoryLength*/
		}
		fv.set(16, temp);

		if(fv.get(4) == 0)
			fv.set(17, 0);
		else
			fv.set(17, fv.get(16)/fv.get(4));

		/*F19 - First 5 acceleration points; medianAcceleration*/
		double accelerationPoints [] = {pairwAcceleration[0],
				pairwAcceleration[1], pairwAcceleration[2],
				pairwAcceleration[3],pairwAcceleration[4],
				pairwAcceleration[5]};
		fv.set(18, ArrayUtil.percentile(accelerationPoints, 0.50));

		/*F20-22: midPressure, midArea, phoneOrientation*/
		fv.set(19, this.touchPoints.get(numPoints/2).pressure);
		fv.set(20, this.touchPoints.get(numPoints/2).width);
		fv.set(21, this.touchPoints.get(numPoints/2).orientation);

		/*F23 - Direction Flag. up, down, left, right are 0,1,2,3*/
		fv.set(22, 1);
		double xDiff = fv.get(2) - fv.get(0);
		double yDiff = fv.get(3) - fv.get(1);
		if (Math.abs(xDiff) > Math.abs(yDiff))
			if (xDiff < 0)
				fv.set(22, 2); //left
			else
				fv.set(22, 3); //right
		else
			if (yDiff < 0)
				fv.set(22, 0); //up

		/*F24-25: distToTrajRatio; averageDirection*/
		if (fv.get(16) == 0)
			fv.set(23, 0);
		else
			fv.set(23, fv.get(6)/fv.get(16));

		fv.set(24 ,ComplexNumbers.circ_mean(pairwAngle));

		/*F26-29 - Largest/20%/50%/80% deviation from end-to-end line*/
		double xVek [] = new double[numPoints];
		double yVek [] = new double[numPoints];
		for (int i = 0; i < numPoints - 1; i++) {
			xVek[i] = this.touchPoints.get(i).xVal - fv.get(0);
			yVek[i] = this.touchPoints.get(i).yVal - fv.get(1);
		}
		double perVek[] = {yVek[yVek.length-1], xVek[xVek.length-1] - 1, 0};
		temp = Math.sqrt(Math.pow(perVek[0], 2) + Math.pow(perVek[1], 2));
		if (temp == 0)
			perVek[0] = perVek[1] = perVek[2]  = 0;
		else {
			perVek[0] /= temp;
			perVek[1] /= temp;
			perVek[2] /= temp;
		}

		double absProj [] = new double[numPoints];
		for (int i = 0; i < numPoints - 1; i++) 
			absProj[i] = Math.abs(xVek[i] * perVek[0] + yVek[i] * perVek[1]);
		fv.set(25, ArrayUtil.max(absProj));
		fv.set(26, ArrayUtil.percentile(absProj, 0.2));
		fv.set(27, ArrayUtil.percentile(absProj, 0.5));
		fv.set(28, ArrayUtil.percentile(absProj, 0.8));
	}
}
