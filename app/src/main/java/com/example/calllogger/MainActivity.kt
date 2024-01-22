package com.example.calllogger

import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calllogger.GlobalData.callLogHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

// Global object to store call history so that it can be easily accessed in DetailsActivity
object GlobalData {
    var callLogHistory: ConcurrentHashMap<String, MutableList<MyCallLog>> = ConcurrentHashMap()
}

class MainActivity : AppCompatActivity(), MyAdapter.OnItemClickListener {
    private var callLogs: MutableList<MyCallLog> = mutableListOf()
    private var currentPage = 1
    private val pageSize = 10
    private var isEnd = false
    // Request code for reading call logs permission
    object REQUEST_CODE {
        const val code = 100
    }

    private companion object {
        const val COLUMN_NAME = CallLog.Calls.CACHED_NAME
        const val COLUMN_NUMBER = CallLog.Calls.NUMBER
        const val COLUMN_TYPE = CallLog.Calls.TYPE
        const val COLUMN_DATE = CallLog.Calls.DATE
        const val COLUMN_DURATION = CallLog.Calls.DURATION
    }

    private lateinit var myAdapter: MyAdapter
    private lateinit var recyclerView: RecyclerView

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)

        // If permission is not granted, request it
        if (!isPermissionGranted()) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(
                    android.Manifest.permission.READ_CALL_LOG
                ),
                REQUEST_CODE.code
            )
        } else {
            setupRecyclerView()
            fetchData()
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchData() {
        lifecycleScope.launch(Dispatchers.IO) {
            getCallLogs()
        }
    }

    // Query call logs
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun getCallLogs() {
        // Access call logs using ContentResolver
        currentPage+=pageSize
        val queryBundle = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "${CallLog.Calls.DATE} DESC")
            putInt(ContentResolver.QUERY_ARG_LIMIT, pageSize)
            putInt(ContentResolver.QUERY_ARG_OFFSET, currentPage )
        }
        val contentResolver: ContentResolver = contentResolver
        val cursor: Cursor? = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null,
            queryBundle,
            null
        )

        cursor?.use {
            // Get column indices
            val numberIndex = it.getColumnIndex(COLUMN_NUMBER)
            val nameIndex = it.getColumnIndex(COLUMN_NAME)
            val typeIndex = it.getColumnIndex(COLUMN_TYPE)
            val dateIndex = it.getColumnIndex(COLUMN_DATE)
            val durationIndex = it.getColumnIndex(COLUMN_DURATION)

            if (it.moveToFirst()) {
                do {
                    // Check if column index is valid before accessing data
                    val number = if (numberIndex != -1) it.getString(numberIndex) else ""
                    val name = if (nameIndex != -1) it.getString(nameIndex) else ""
                    val type = if (typeIndex != -1) it.getString(typeIndex) else ""
                    val date = if (dateIndex != -1) it.getString(dateIndex) else ""
                    val duration = if (durationIndex != -1) it.getString(durationIndex) else ""

                    // Create object
                    val logObj = MyCallLog(number, name, duration, type, date)

                    // Update the call log history for all fetched items
                    updateCallLogHistory(logObj)

                    // Check if the call log with the same number already exists
                    val existingLog = callLogs.find { it.number == logObj.number }

                    if (existingLog != null) {
                        // Update the existing call log entry
                        callLogHistory[existingLog.number]?.add(logObj)
                    } else {
                        // Create a new entry in call log history
                        val obj = MyCallLog(number, name, duration, type, date)
                        callLogs.add(obj)
                    }

                } while (it.moveToNext())

                // Display call logs in RecyclerView
                withContext(Dispatchers.Main) {
                    displayCallLogs()
                }
            } else {
                isEnd = true
            }
        }
    }

    // Check for permission is granted or not
    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this@MainActivity,
            android.Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Update call log history with the given log object
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

    // Display call logs in RecyclerView
    private fun displayCallLogs() {
        if (!::myAdapter.isInitialized) {
            myAdapter = MyAdapter(this@MainActivity, callLogs)
            myAdapter.onItemClickListener = this@MainActivity
            recyclerView.adapter = myAdapter

            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    Log.i("currentPage", currentPage.toString())
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                    val totalItemCount = layoutManager.itemCount

                    // Fetch more items when the user is near the end of the list
                    if (lastVisibleItemPosition + 1 == totalItemCount && !isEnd) {
                        fetchData()
                    }
                    Log.i("currentPage", currentPage.toString())
                }
            })
        } else {
            myAdapter.notifyDataSetChanged()
        }
    }
}
