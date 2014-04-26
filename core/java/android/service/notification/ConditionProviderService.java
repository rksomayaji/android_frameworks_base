/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.service.notification;

import android.annotation.SdkConstant;
import android.app.INotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.os.ServiceManager;
import android.util.Log;

/**
 * A service that provides conditions about boolean state.
 * <p>To extend this class, you must declare the service in your manifest file with
 * the {@link android.Manifest.permission#BIND_CONDITION_PROVIDER_SERVICE} permission
 * and include an intent filter with the {@link #SERVICE_INTERFACE} action. For example:</p>
 * <pre>
 * &lt;service android:name=".MyConditionProvider"
 *          android:label="&#64;string/service_name"
 *          android:permission="android.permission.BIND_CONDITION_PROVIDER_SERVICE">
 *     &lt;intent-filter>
 *         &lt;action android:name="android.service.notification.ConditionProviderService" />
 *     &lt;/intent-filter>
 * &lt;/service></pre>
 *
 * @hide
 */
public abstract class ConditionProviderService extends Service {
    private final String TAG = ConditionProviderService.class.getSimpleName()
            + "[" + getClass().getSimpleName() + "]";

    private Provider mProvider;
    private INotificationManager mNoMan;

    /**
     * The {@link Intent} that must be declared as handled by the service.
     */
    @SdkConstant(SdkConstant.SdkConstantType.SERVICE_ACTION)
    public static final String SERVICE_INTERFACE
            = "android.service.notification.ConditionProviderService";

    abstract public void onConnected();
    abstract public void onRequestConditions(int relevance);
    abstract public void onSubscribe(Uri conditionId);
    abstract public void onUnsubscribe(Uri conditionId);

    private final INotificationManager getNotificationInterface() {
        if (mNoMan == null) {
            mNoMan = INotificationManager.Stub.asInterface(
                    ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        }
        return mNoMan;
    }

    public final void notifyConditions(Condition[] conditions) {
        if (!isBound()) return;
        try {
            getNotificationInterface().notifyConditions(getPackageName(), mProvider, conditions);
        } catch (android.os.RemoteException ex) {
            Log.v(TAG, "Unable to contact notification manager", ex);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (mProvider == null) {
            mProvider = new Provider();
        }
        return mProvider;
    }

    private boolean isBound() {
        if (mProvider == null) {
            Log.w(TAG, "Condition provider service not yet bound.");
            return false;
        }
        return true;
    }

    private final class Provider extends IConditionProvider.Stub {
        private final ConditionProviderService mService = ConditionProviderService.this;

        @Override
        public void onConnected() {
            try {
                mService.onConnected();
            } catch (Throwable t) {
                Log.w(TAG, "Error running onConnected", t);
            }
        }

        @Override
        public void onRequestConditions(int relevance) {
            try {
                mService.onRequestConditions(relevance);
            } catch (Throwable t) {
                Log.w(TAG, "Error running onRequestConditions", t);
            }
        }

        @Override
        public void onSubscribe(Uri conditionId) {
            try {
                mService.onSubscribe(conditionId);
            } catch (Throwable t) {
                Log.w(TAG, "Error running onSubscribe", t);
            }
        }

        @Override
        public void onUnsubscribe(Uri conditionId) {
            try {
                mService.onUnsubscribe(conditionId);
            } catch (Throwable t) {
                Log.w(TAG, "Error running onUnsubscribe", t);
            }
        }
    }
}
