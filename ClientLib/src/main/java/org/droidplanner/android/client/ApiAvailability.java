package org.droidplanner.android.client;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.droidplanner.android.client.utils.InstallServiceDialog;
import org.droidplanner.services.android.impl.api.DroidPlannerService;
import org.droidplanner.services.android.lib.model.IDroidPlannerServices;
import org.droidplanner.services.android.lib.util.version.VersionUtils;

import java.util.List;

/**
 * Helper class to verify that the DroneKit-Android services APK is available and up-to-date
 * Created by Fredia Huya-Kouadio on 7/7/15.
 */
class ApiAvailability {

    private static class LazyHolder {
        private static final ApiAvailability INSTANCE = new ApiAvailability();
    }

    private static final String SERVICES_CLAZZ_NAME = IDroidPlannerServices.class.getName();
    private static final String METADATA_KEY = "org.droidplanner.services.android.lib.version";

    private static final String DEPRECATED_SERVICES_CLAZZ_NAME = "com.o3dr.services.android.lib.model.IDroidPlannerServices";
    private static final String DEPRECATED_METADATA_KEY = "com.o3dr.dronekit.android.core.version";

    public static final int API_AVAILABLE = 0;
    public static final int API_MISSING = 1;
    public static final int API_UPDATE_REQUIRED = 2;

    private static final int INVALID_LIB_VERSION = -1;

    //Private to prevent instantiation
    private ApiAvailability() {
    }

    static ApiAvailability getInstance() {
        return LazyHolder.INSTANCE;
    }

    /**
     * Find and returns the most adequate instance of the services lib.
     *
     * @param context Application context. Must not be null.
     * @return intent Intent used to bind to an instance of the services lib.
     */
    Intent getAvailableServicesInstance(@NonNull final Context context) {
        final PackageManager pm = context.getPackageManager();

        //Check if an instance of the services library is up and running.
        final Intent serviceIntent = new Intent(SERVICES_CLAZZ_NAME);
        final List<ResolveInfo> serviceInfos = pm.queryIntentServices(serviceIntent, PackageManager.GET_META_DATA);
        if(serviceInfos != null && !serviceInfos.isEmpty()){
            for(ResolveInfo serviceInfo : serviceInfos) {
                final Bundle metaData = serviceInfo.serviceInfo.metaData;
                if (metaData == null)
                    continue;

                final int towerLibVersion = metaData.getInt(METADATA_KEY, INVALID_LIB_VERSION);
                if (towerLibVersion != INVALID_LIB_VERSION && towerLibVersion >= VersionUtils.getTowerLibVersion(context)) {
                    serviceIntent.setClassName(serviceInfo.serviceInfo.packageName, serviceInfo.serviceInfo.name);
                    return serviceIntent;
                }
            }
        }

        //TODO: For testing only.. Remove before release for shipping
        final Intent deprecatedServiceIntent = new Intent(DEPRECATED_SERVICES_CLAZZ_NAME);
        final List<ResolveInfo> deprecatedServiceInfos = pm.queryIntentServices(deprecatedServiceIntent, PackageManager.GET_META_DATA);
        if(deprecatedServiceInfos != null && !deprecatedServiceInfos.isEmpty()){
            for(ResolveInfo deprecatedServiceInfo : deprecatedServiceInfos){
                final Bundle deprecatedMetaData = deprecatedServiceInfo.serviceInfo.metaData;
                if(deprecatedMetaData == null)
                    continue;

                final int deprecatedLibVersion = deprecatedMetaData.getInt(DEPRECATED_METADATA_KEY, INVALID_LIB_VERSION);
                if(deprecatedLibVersion != INVALID_LIB_VERSION && deprecatedLibVersion >= VersionUtils.getDeprecatedLibVersion(context)){
                    deprecatedServiceIntent.setClassName(deprecatedServiceInfo.serviceInfo.packageName, deprecatedServiceInfo.serviceInfo.name);
                    return deprecatedServiceIntent;
                }
            }
        }

        //Didn't find any that's up and running. Enable the local one
        DroidPlannerService.enableDroidPlannerService(context, true);
        serviceIntent.setClass(context, DroidPlannerService.class);
        return serviceIntent;
    }

    /**
     * Display a dialog for an error code returned from callback to {@link ApiAvailability#getAvailableServicesInstance(Context)}
     *
     * @param context   Application context
     * @param errorCode Error code returned from callback to
     *                  {@link ApiAvailability#getAvailableServicesInstance(Context)}. If errorCode is API_AVAILABLE, then this does nothing.
     */
    void showErrorDialog(Context context, int errorCode) {
        switch (errorCode) {
            case API_MISSING:
                context.startActivity(new Intent(context, InstallServiceDialog.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(InstallServiceDialog.EXTRA_REQUIREMENT, InstallServiceDialog.REQUIRE_INSTALL));
                break;

            case API_UPDATE_REQUIRED:
                context.startActivity(new Intent(context, InstallServiceDialog.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(InstallServiceDialog.EXTRA_REQUIREMENT, InstallServiceDialog.REQUIRE_UPDATE));
                break;
        }
    }

}
