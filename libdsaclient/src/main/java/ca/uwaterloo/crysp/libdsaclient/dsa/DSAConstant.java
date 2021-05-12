package ca.uwaterloo.crysp.libdsaclient.dsa;

public class DSAConstant {
    public static final String REMOTE_SERVICE_NAME = "ca.uwaterloo.crysp.sharingmodeservice";


    public static final String EXTRA_FIELD_STATUS = "SharingStatus";
    public static final String EXTRA_FIELD_IA_RESULT = "IAResult";
    public static final String EXTRA_FIELD_IA_SCORE = "IAScore";

    public static final String ACTION_ACQUIRE_SHARING_STATUS = "Acquire sharing status";
    public static final String ACTION_INITIALIZE_SHARING_STATUS = "Initialize status";
    public static final String ACTION_UPDATE_IA_RESULT = "Update IA result";


    public static final int SHARING_STATUS_UNAVAILABLE = 0;
    public static final int SHARING_STATUS_NO_SHARING = 1;
    public static final int SHARING_STATUS_GESTURE_DETECTED = 4;
    public static final int SHARING_STATUS_SHARING_CONFIRMED =5;
    public static final int SHARING_STATUS_RETURN_DETECTED = 8;
    public static final int SHARING_STATUS_RETURN_CONFIRMED = 9;
}
