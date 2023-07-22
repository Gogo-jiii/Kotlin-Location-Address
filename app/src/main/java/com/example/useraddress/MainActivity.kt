package com.example.useraddress

import android.Manifest
import android.annotation.SuppressLint
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.useraddress.databinding.ActivityMainBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.io.IOException
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    false
                ) || permissions.getOrDefault(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    false
                ) -> {
                    Toast.makeText(this, "location access granted", Toast.LENGTH_SHORT).show()

                    if (isLocationEnabled()) {
                        val result = fusedLocationClient.getCurrentLocation(
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                            CancellationTokenSource().token
                        )
                        result.addOnCompleteListener {
                            val location =
                                "Latitude: " + it.result.latitude + "\n" + "Longitude: " + it.result.longitude

                            getAddress(it.result)
                        }
                    } else {
                        Toast.makeText(this, "Please turn ON the location.", Toast.LENGTH_SHORT)
                            .show()
                        createLocationRequest()
                    }

                }

                else -> {
                    Toast.makeText(this, "no location access", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnGetAddress.setOnClickListener {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun getAddress(location: Location) {
        val geocoder = Geocoder(this@MainActivity, Locale.getDefault())

        try {
            val addresses = geocoder.getFromLocation(
                location.latitude, location.longitude, 1
            )
            val address = addresses!![0]
            val strAddress = """
        Addressline: ${address.getAddressLine(0)}
        Admin Area: ${address.adminArea}
        Country Name: ${address.countryName}
        Feature Name: ${address.featureName}
        Locality: ${address.locality}
        
        """.trimIndent()
            binding.textView.text = strAddress
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        try {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun createLocationRequest() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000
        ).setMinUpdateIntervalMillis(5000).build()

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
        }

        task.addOnFailureListener { e ->
            if (e is ResolvableApiException) {
                try {
                    e.startResolutionForResult(
                        this,
                        100
                    )
                } catch (_: java.lang.Exception) {
                }
            }
        }
    }

}