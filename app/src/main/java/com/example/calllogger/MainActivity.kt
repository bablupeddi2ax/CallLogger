package com.example.calllogger

import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.CallLog
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calllogger.GlobalData.callLogHistory

// Global object to store call history so that it can be easily accessed in DetailsActivity
object GlobalData {
    var callLogHistory: HashMap<String, MutableList<MyCallLog>> = HashMap()
}

class MainActivity : AppCompatActivity(), MyAdapter.OnItemClickListener {
    private var callLogs: MutableList<MyCallLog> = mutableListOf()

    // Request code for reading call logs  permission
    object REQUESTCODE {
        const val code = 100
    }

    private lateinit var myAdapter: MyAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)

        // If permission is not granted request
        if (!isPermissionGranted()) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(
                    android.Manifest.permission.READ_CALL_LOG
                ),
                REQUESTCODE.code
            )
        } else {
            //Access call logs using ContentResolver
            val contentResolver: ContentResolver = contentResolver
            val cursor: Cursor? = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                null,
                null,
                null,
                CallLog.Calls.DATE + " DESC"  // Sort by date in descending order to get the recent logs first
            )

            cursor?.use {
                // Get column indices
                val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
                val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
                val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)

                if (it.moveToFirst()) {
                    do {
                        // Check if column index is valid before accessing data
                        val number = if (numberIndex != -1) it.getString(numberIndex) else ""
                        val name = if (nameIndex != -1) it.getString(nameIndex) else ""
                        val type = if (typeIndex != -1) it.getString(typeIndex) else ""
                        val date = if (dateIndex != -1) it.getString(dateIndex) else ""
                        val duration = if (durationIndex != -1) it.getString(durationIndex) else ""

                        // create object
                        val logObj = MyCallLog(number, name, duration, type, date)

                        // Check if the call log with the same number already exists
                        val existingLog = callLogs.find { it.number == logObj.number }

                        if (existingLog != null) {
                            // Update the existing call log entry
                            callLogHistory[existingLog.number]?.add(logObj)
                        } else {
                            // Create a new entry in call log history
                            updateCallLogHistory(logObj)
                            val obj = MyCallLog(number, name, duration, type, date)
                            callLogs.add(obj)
                        }
                    } while (it.moveToNext())

                    // Display call logs in RecyclerView
                    displayCallLogs()
                }
            }
        }
    }
    // check for permission is granted or not
    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this@MainActivity,
            android.Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    // update call log history with the given log object
    private fun updateCallLogHistory(logObj: MyCallLog) {
        val phoneNumber = logObj.number

        if (callLogHistory.containsKey(phoneNumber)) {
            // Phone number exists, add the current call log to the existing list
            callLogHistory[phoneNumber]?.add(logObj)
        } else {
            // Phone number doesn't exist, create a new list with the current call log
            val newCallLogList = mutableListOf(logObj)
            callLogHistory[phoneNumber] = newCallLogList
        }
    }
    // Handle item click
    override fun onItemClick(item: MyCallLog) {
        val intent = Intent(this@MainActivity, DetailsActivity::class.java)
        intent.putExtra("number", item.number)
        intent.putExtra("name", item.name)
        intent.putExtra("duration", item.duration)
        intent.putExtra("type", item.type)
        intent.putExtra("date", item.date)
        startActivity(intent)
    }

    // display call logs in recyclerview
    private fun displayCallLogs() {
        // Transform callLogHistory into a list of pairs for RecyclerView
        val groupedLogs = callLogHistory.toList()

        if (groupedLogs.isNotEmpty()) {
            myAdapter = MyAdapter(this@MainActivity, callLogs)
            myAdapter.onItemClickListener = this
            recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
            recyclerView.adapter = myAdapter
        }
    }
}

