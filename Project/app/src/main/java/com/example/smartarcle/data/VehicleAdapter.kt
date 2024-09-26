package com.example.smartarcle.data

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.smartarcle.R

class VehicleAdapter(private var vehicleList: List<VehicleData>) : RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder>()  {
    class VehicleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val plate: TextView = itemView.findViewById(R.id.tv_item_plate_data)
        val name: TextView = itemView.findViewById(R.id.tv_item_name_data)
        val address: TextView = itemView.findViewById(R.id.tv_item_address_data)
        val brand: TextView = itemView.findViewById(R.id.tv_item_brand_data)
        val colour: TextView = itemView.findViewById(R.id.tv_item_colour_data)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_vehicle_profile_security, parent, false)
        return VehicleViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        val vehicle = vehicleList[position]
        holder.plate.text = vehicle.plate
        holder.name.text = vehicle.name
        holder.address.text = vehicle.address
        holder.brand.text = vehicle.brand
        holder.colour.text = vehicle.colour
    }

    override fun getItemCount(): Int {
        return vehicleList.size
    }
}