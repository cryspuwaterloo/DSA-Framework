package ca.uwaterloo.crysp.sharingmodeservice.gesture;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.rank.Median;

public class MathHelper {
    public static float srss(float[] items) {
        // square root of the sum of squares
        float result = 0;
        for(float item: items) {
            result += item * item;
        }
        return (float) Math.sqrt(result);
    }

    public static float absFLoat(float v) {
        if (v < 0)
            return -v;
        else
            return v;
    }


    /*
    feature functions (some are from Apache Commons)
     */

    public static double median(double[] values) {
        Median median_cal = new Median();
        return median_cal.evaluate(values);
    }

    public static double range(double[] values) {
        return StatUtils.max(values) - StatUtils.min(values);
    }

    public static double[] cumsum(double[] values) {
        double[] result = new double[values.length];
        double tmp = 0;
        for(int i = 0; i < values.length; ++i) {
            tmp += values[i];
            result[i] = tmp;
        }
        return result;
    }

    public static double scusum(double[] values, double scale) {
        return StatUtils.sum(cumsum(values)) * scale;
    }

    public static double mad(double[] values) {
        double[] tmp = new double[values.length];
        double med = median(values);
        for(int i = 0; i <  values.length; ++i) {
            tmp[i] = Math.abs(values[i] - med);
        }
        return median(tmp);
    }

    public static double std(double[] values) {
        StandardDeviation std_cal = new StandardDeviation();
        return std_cal.evaluate(values);
    }

    public static double rms(double[] values) {
        double tmp = 0;
        for(double value: values) {
            tmp += value * value;
        }
        return Math.sqrt(tmp/values.length);
    }

    public static double absum(double[] values) {
        double result = 0;
        for(double value: values) {
            result += Math.abs(value);
        }
        return result;
    }


    public static double entropy(double[] values) {
        // normalization
        double absSum = absum(values);
        double result = 0;
        for(double value: values) {
            result += Math.abs(value/absSum) * Math.log(Math.abs(value/absSum));
        }
        return -result;
    }

    public static double cov(double[] va, double[] vb) {
        double cov_value = new Covariance().covariance(va, vb);
        return cov_value / std(va) / std(vb);
    }
}
