package com.example.scrollersdashboard

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private var tasks: List<Any>,
    private val onToggle: (Any) -> Unit,
    private val onDelete: (Any) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    fun updateTasks(newTasks: List<Any>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alert_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        val isCompleted: Boolean
        val text: String

        if (task is HabitTask) {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            isCompleted = task.lastCompletedDate == today
            text = task.title
        } else if (task is TodoTask) {
            isCompleted = task.isCompleted
            text = task.title
        } else {
            return
        }

        holder.tvTaskText.text = text
        if (isCompleted) {
            holder.tvTaskText.paintFlags = holder.tvTaskText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvTaskText.setTextColor(holder.itemView.context.getColor(R.color.gray_400))
            holder.ivCheck.visibility = View.VISIBLE
            holder.checkContainer.setBackgroundResource(R.drawable.bg_task_check_circle_selected)
            holder.taskContainer.isActivated = true
        } else {
            holder.tvTaskText.paintFlags = holder.tvTaskText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.tvTaskText.setTextColor(holder.itemView.context.getColor(R.color.white))
            holder.ivCheck.visibility = View.GONE
            holder.checkContainer.setBackgroundResource(R.drawable.bg_task_check_circle_unselected)
            holder.taskContainer.isActivated = false
        }

        holder.checkContainer.setOnClickListener { onToggle(task) }
        holder.btnDelete.setOnClickListener { onDelete(task) }
    }

    override fun getItemCount(): Int = tasks.size

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val taskContainer: View = view.findViewById(R.id.taskContainer)
        val checkContainer: View = view.findViewById(R.id.checkContainer)
        val ivCheck: ImageView = view.findViewById(R.id.ivCheck)
        val tvTaskText: TextView = view.findViewById(R.id.tvTaskText)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }
}
