package com.example.terravive

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.terravive.OrganizerActivityAdapter
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class OrganizerActivitiesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: OrganizerActivityAdapter
    private val activityList = mutableListOf<ActivityItemModel>()
    private val db = FirebaseFirestore.getInstance()
    private val handler = Handler(Looper.getMainLooper())
    private val undoTimers = mutableMapOf<String, Runnable>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_organizer_activities, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewActivities)

        adapter = OrganizerActivityAdapter(
            activityList,
            { activity -> editActivity(activity) },    // onEditClick
      //      { activity -> undoActivity(activity) },     // onUndoClick (assuming you have this function)
            { activity -> deleteActivity(activity) }     // onDeleteClick
        )

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        loadActivities()

        // Handle FAB or button for adding activity (optional)
        view.findViewById<View>(R.id.btnAddActivity)?.setOnClickListener {
            showCreateActivityDialog()
        }

        return view
    }

    private fun loadActivities() {
        db.collection("activities_approved").get().addOnSuccessListener { result ->
            activityList.clear()
            for (doc in result) {
                val item = doc.toObject(ActivityItemModel::class.java)
                activityList.add(item)
            }
            adapter.notifyDataSetChanged()
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Error loading activities: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCreateActivityDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_activity, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.editTextTitle)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.editTextDescription)

        AlertDialog.Builder(requireContext())
            .setTitle("Create Activity")
            .setView(dialogView)
            .setPositiveButton("Submit") { _, _ ->
                val id = UUID.randomUUID().toString()
                val newActivity = ActivityItemModel(
                    id = id,
                    title = titleInput.text.toString(),
                    description = descriptionInput.text.toString(),
                    isApproved = false
                )
                submitActivity(newActivity)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitActivity(activity: ActivityItemModel) {
        db.collection("activities_approved").document(activity.id).set(activity).addOnSuccessListener {
            activityList.add(activity)
            adapter.notifyItemInserted(activityList.size - 1)

            // Allow undo within 90s
            val undoRunnable = Runnable {
                undoTimers.remove(activity.id) // Remove from undo map after time passes
            }

            undoTimers[activity.id] = undoRunnable
            handler.postDelayed(undoRunnable, 90_000) // 1 min 30 sec
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Error submitting activity: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteActivity(activity: ActivityItemModel) {
        // Allow undo only if within 90s
        val canUndo = undoTimers.containsKey(activity.id)
        db.collection("activities_approved").document(activity.id).delete().addOnSuccessListener {
            activityList.remove(activity)
            adapter.notifyDataSetChanged()

            if (canUndo) {
                undoTimers[activity.id]?.let { handler.removeCallbacks(it) }
                undoTimers.remove(activity.id)
            }
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Error deleting activity: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun editActivity(activity: ActivityItemModel) {
        if (!undoTimers.containsKey(activity.id)) {
            Toast.makeText(context, "This activity can no longer be edited.", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_activity, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.editTextTitle)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.editTextDescription)

        titleInput.setText(activity.title)
        descriptionInput.setText(activity.description)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Activity")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val updatedActivity = activity.copy(
                    title = titleInput.text.toString(),
                    description = descriptionInput.text.toString()
                )
                db.collection("activities_approved").document(updatedActivity.id).set(updatedActivity)
                    .addOnSuccessListener {
                        val index = activityList.indexOfFirst { it.id == updatedActivity.id }
                        if (index != -1) {
                            activityList[index] = updatedActivity
                            adapter.notifyItemChanged(index)
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error saving activity: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
