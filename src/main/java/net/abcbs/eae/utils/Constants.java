package net.abcbs.eae.utils;

public final class Constants {
    public static final String SYSTEM_NAME = "RPADispatcherService";
    public static final String AUTH_KEY = " ";
    public static final String AUTH_PASS_PHRASE_DEV = " ";
    public static final String SERVER_ENVIRONMENT_VARIABLE = "IsSharedServerEnv";
    public static final String ORCH_TOKEN_URL = "identity/connect/token";
    public static final String ORCH_ADD_QUEUE_URL = "odata/Queues/UiPathODataSvc.AddQueueItem";
    public static final String RETRO_TERMS_POWER_EXCEPTION_QUEUE_WM = "BANA_WM_ADJRPTS_RETRO_EXCPT";
    public static final String RETRO_TERMS_POWER_EXCEPTION_QUEUE_TY = "BANA_TY_ADJRPTS_RETRO_EXCPT";
    public static final String DIAGREVIEW_POWER_EXCEPTION_QUEUE_INSTATE_WM = "BANA_WM_ISCLM_DIAGREV_EXCPTN";
    public static final String DIAGREVIEW_POWER_EXCEPTION_QUEUE_ITS_WM = "BANA_WM_ITSCLM_DIAGREV_EXCPTN";
    public static final String DIAGREVIEW_POWER_EXCEPTION_QUEUE_INSTATE_TY = "BANA_TY_ISCLM_DIAGREV_EXCPTN";
    public static final String DIAGREVIEW_POWER_EXCEPTION_QUEUE_ITS_TY =  "BANA_TY_ITSCLM_DIAGREV_EXCPTN";
    public static final String BLUE_ADVANTAGE_FOLDER = "InternalOperations/BlueAdvantage";

    private Constants() {
        throw new UnsupportedOperationException("This is a String constant class and cannot be instantiated");
    }
}
