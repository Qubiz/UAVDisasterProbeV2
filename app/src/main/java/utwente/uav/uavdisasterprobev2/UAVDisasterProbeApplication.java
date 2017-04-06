package utwente.uav.uavdisasterprobev2;

import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * Created by Mathijs on 06/04/2017.
 */

public class UAVDisasterProbeApplication extends Application {

    public static final String FLAG_CONNECTION_CHANGE = "UAVDP_APPLICATION_CONNECTION_CHANGE";

    private static BaseProduct product;

    private Handler handler;
    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            sendBroadcast(intent);
        }
    };
    private BaseComponent.ComponentListener componentListener = new BaseComponent.ComponentListener() {
        @Override
        public void onConnectivityChange(boolean isConnected) {
            notifyStatusChange();
        }
    };
    private BaseProduct.BaseProductListener baseProductListener = new BaseProduct.BaseProductListener() {
        @Override
        public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent, BaseComponent newComponent) {
            if (newComponent != null) {
                newComponent.setComponentListener(componentListener);
            }

            notifyStatusChange();
        }

        @Override
        public void onConnectivityChange(boolean isConnected) {
            notifyStatusChange();
        }
    };
    /**
     * When starting SDK services, an instance of interface DJISDKManager.DJISDKManagerCallback will be used to listen to
     * the SDK Registration result and the product changing.
     */
    private DJISDKManager.SDKManagerCallback sdkManagerCallback = new DJISDKManager.SDKManagerCallback() {
        @Override
        public void onRegister(DJIError error) {
            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "REGISTRATION SUCCESS", Toast.LENGTH_LONG).show();
                    }
                });

                DJISDKManager.getInstance().startConnectionToProduct();
            } else {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "REGISTRATION FAILED", Toast.LENGTH_LONG).show();
                    }
                });
            }

            Log.e(UAVDisasterProbeApplication.class.getSimpleName(), error.toString());
        }

        @Override
        public void onProductChange(BaseProduct oldProduct, BaseProduct newProduct) {
            product = newProduct;
            if (product != null) {
                product.setBaseProductListener(baseProductListener);
            }

            notifyStatusChange();
        }
    };

    public static synchronized BaseProduct getProductInstance() {
        if (product == null) {
            product = DJISDKManager.getInstance().getProduct();
        }

        return product;
    }

    public static boolean isAircraftConnected() {
        return getProductInstance() != null && getProductInstance() instanceof Aircraft;
    }

    public static synchronized Camera getCameraInstance() {
        if (getProductInstance() == null) return null;

        Camera camera = null;

        if (isAircraftConnected()) {
            camera = ((Aircraft) getProductInstance()).getCamera();
        }

        return camera;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler(Looper.getMainLooper());

        DJISDKManager.getInstance().registerApp(this, sdkManagerCallback);
    }

    private void notifyStatusChange() {
        handler.removeCallbacks(updateRunnable);
        handler.postDelayed(updateRunnable, 500);
    }
}
