package com.example.terravive

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView

class OrganizerActivityAdapter(
    private val activityList: MutableList<ActivityItemModel>, // MutableList to allow updates
    private val onEditClick: (ActivityItemModel) -> Unit,
    private val onUndoClick: (ActivityItemModel) -> Unit,
    private val onDeleteClick: (ActivityItemModel) -> Unit
) : RecyclerView.Adapter<OrganizerActivityAdapter.OrganizerActivityViewHolder>() {

    inner class OrganizerActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.tvActivityTitle)
        val dateText: TextView = itemView.findViewById(R.id.tvActivityDate)
        val locationText: TextView = itemView.findViewById(R.id.tvActivityLocation)
        val editButton: Button = itemView.findViewById(R.id.btnEditActivity)
        val deleteButton: Button = itemView.findViewById(R.id.btnDeleteActivity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrganizerActivityViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_organizer_activity, parent, false)
        return OrganizerActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrganizerActivityViewHolder, position: Int) {
        val activity = activityList[position]
        holder.titleText.text = "Title: ${activity.title}"  // Corrected from name to title
        holder.dateText.text = "Date: ${activity.date}"
        holder.locationText.text = "Location: ${activity.location}"

        holder.editButton.setOnClickListener {
            onEditClick(activity)
        }


        holder.deleteButton.setOnClickListener {
            onDeleteClick(activity)
        }
    }

    override fun getItemCount(): Int = activityList.size

    // Method to update activities in the list
    fun updateActivities(newActivities: List<ActivityItemModel>) {
        activityList.clear()  // Clears the existing list
        activityList.addAll(newActivities)  // Adds the new activities
        notifyDataSetChanged()  // Notifies the adapter that data has changed
    }
}
