package myGameEngine.Tools;

/**
 * Helper for rage float/double conversions
 */
public class ArrayHelper {
    public static double[] toDoubleArray(float[] in) {
        int size = in.length;
        double[] ret = new double[size];
        for(int i = 0; i < size; ++i) ret[i] = (double) in[i];
        return ret;
    }
    
    public static float[] toFloatArray(double[] in) {
        int size = in.length;
        float[] ret = new float[size];
        for(int i = 0; i < size; ++i) ret[i] = (float) in[i];
        return ret;
    }
}
