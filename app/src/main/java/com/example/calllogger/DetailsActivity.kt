package com.example.calllogger

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.w3c.dom.Text

class DetailsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var myAdapter: MyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details_activty)
        // get the log details
        val intent = intent
        val number = intent.getStringExtra("number")
        val name = intent.getStringExtra("name")

        // assign the contact name and contact  number
        val txtName = findViewById<TextView>(R.id.txtName)
        val txtNumber = findViewById<TextView>(R.id.txtNumber)
        txtName.text = name
        txtNumber.text = number

        // fetch history of the current number (grouping call logs )
        val history = GlobalData.callLogHistory[number]


        recyclerView = findViewById(R.id.detailRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this@DetailsActivity)
        // check if not null or empty
       history?.let{
            myAdapter = MyAdapter(this@DetailsActivity, history)
            recyclerView.adapter = myAdapter
            myAdapter.onItemClickListener = null
        }


    }
}