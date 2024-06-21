package circuitlord.reactivemusic;

public class MyMath {


    public static float lerpConstant(float currentValue, float targetValue, float lerpRate) {

        lerpRate = Math.abs(lerpRate);
        // If need to get smaller then invert
        if (targetValue < currentValue) lerpRate = -lerpRate;

        float result = currentValue + lerpRate;

        if ((lerpRate > 0 && result > targetValue) || (lerpRate < 0 && result < targetValue)) {
            result = targetValue;
        }

        return result;
    }

}
