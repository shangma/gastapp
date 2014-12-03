/*
 * Copyright 2011 Greg Milette and Adam Stroud
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package root.gast.playground.location;

import root.gast.location.LocationBroadcastReceiver;
import android.content.Context;
import android.location.Location;
import android.util.Log;

/**
 * Broadcast receiver that will perform filtering algorithms on location information
 * that is received from the location service.
 * 
 * @author Adam Stroud &#60;<a href="mailto:adam.stroud@gmail.com">adam.stroud@gmail.com</a>&#62;
 */
public abstract class FilteringLocationBroadcastReceiver extends LocationBroadcastReceiver
{
    private static final String TAG = "FilteringLocationBroadcastReceiver";
    private static final int TIME_THRESHOLD = 30000; // 30 sec.
    private static final int ACCURACY_PERCENT = 10;
    private static final int VELOCITY_THRESHOLD = 200; // m/s

    @Override
    public void onLocationChanged(Context context, Location location)
    {
        Point lastPoint =
                PointDatabaseManager.getInstance(context).retrieveLatestPoint();
        if (lastPoint == null)
        {
            Log.d(TAG, "Adding point");
            onFilteredLocationChanged(context, location);
        }
        else
        {
            float currentAccuracy = location.getAccuracy();
            float previousAccuracy = lastPoint.getAccuracy();
            
            Point point =
                    PointDatabaseManager.getInstance(context).retrieveLatestPoint();
     
            // True IFF accuracy is greater, but limited to 10% of the previous
            // accuracy and new point was generated by the same provider
            float accuracyDifference = Math.abs(previousAccuracy - currentAccuracy);
            boolean lowerAccuracyAcceptable = currentAccuracy > previousAccuracy
                    && lastPoint.getProvider().equals(location.getProvider())
                    && (accuracyDifference <= previousAccuracy / ACCURACY_PERCENT);
            
            float[] results = new float[1];
            
            Location.distanceBetween(point.getLatitude(),
                                     point.getLongitude(),
                                     location.getLatitude(),
                                     location.getLongitude(),
                                     results);
            
            float velocity =
                    results[0] / ((location.getTime() - point.getTime()) / 1000);
            
            // Accept the new point if:
            // * The velocity seems reasonable (point did not jump)and one of the
            //   following:
            //   * It has a better accuracy
            //   * The app has not accepted a point in TIME_THRESHOLD
            //   * It's worse accuracy is still acceptable
            if (velocity <= VELOCITY_THRESHOLD
                    && (currentAccuracy < previousAccuracy
                    || (location.getTime() - lastPoint.getTime()) > TIME_THRESHOLD
                    || lowerAccuracyAcceptable))
            {
                Log.d(TAG, "Adding point");
                onFilteredLocationChanged(context, location);
            }
            else
            {
                Log.d(TAG, "Ignoring point");
            }
        }
    }

    protected abstract void onFilteredLocationChanged(Context context,
            Location location);
}
